package cloud.marisa.picturebackend.sharding;

import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.enums.MrsSpaceType;
import cloud.marisa.picturebackend.enums.SpaceLevelEnum;
import cloud.marisa.picturebackend.service.ISpaceService;
import cloud.marisa.picturebackend.util.EnumUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import lombok.extern.log4j.Log4j2;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.infra.metadata.database.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author MarisaDAZE
 * @description 动态分库分表管理器
 * @date 2025/4/14
 */
@Log4j2
//@Component
public class DynamicShardingManager {

    // @Value("${spring.shardingsphere.datasource.names}")
    // private String DB_NAME;

    @Resource
    private DataSource dataSource;

    @Resource
    private ISpaceService spaceService;

    private static final String LOGIC_TABLE_NAME = "picture";

    /**
     * 配置文件中的数据库名称
     * picture_backend
     */
    private static final String DATABASE_NAME = "logic_db";

//    @PostConstruct
    public void init() {
        log.info("初始化动态分库分表...");
        updateShardingTableNodes();
    }

    /**
     * 动态获取所有的图片表名
     * <p>包括初始 picture 表和分表 picture_{spaceId}</p>
     *
     * @return 图片表名List
     */
    private Set<String> fetchAllPictureTableNames() {
        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
        // 查到所有空间类型是团队空间的（他们都是付费用户，可以享有一个专门的表）
        queryWrapper.eq(Space::getSpaceType, MrsSpaceType.TEAM.getValue())
                .select(Space::getId);
        // 根据ID进行过滤，然后拼接上默认表名获得最终分表名称
        Set<String> dynamicTableNames = spaceService.getBaseMapper()
                .selectObjs(queryWrapper)
                .stream()
                .map(id -> id instanceof Long ? Long.parseLong(id.toString()) : null)
                .filter(ObjUtil::isNotNull)
                .map(id -> LOGIC_TABLE_NAME + "_" + id)
                .collect(Collectors.toSet());
        // 添加默认表名
        dynamicTableNames.add(LOGIC_TABLE_NAME);
        return dynamicTableNames;
    }

    /**
     * 动态更新图片表节点
     * <p>更新 ShardingSphere 和 actual-data-nodes </p>
     */
    private void updateShardingTableNodes() {
        Set<String> tableNames = fetchAllPictureTableNames();
        String dataNodes = tableNames.stream()
                .map(name -> "picture_backend." + name)
                .collect(Collectors.joining(","));
        log.info("分片表节点 actual-data-nodes 的配置: " + dataNodes);

        ContextManager contextManager = getContextManager();
        if (contextManager == null) {
            throw new NullPointerException("ContextManager is null");
        }
        ShardingSphereRuleMetaData ruleMetaData = contextManager.getMetaDataContexts()
                .getMetaData()
                .getDatabases()
                .get(DATABASE_NAME)
                .getRuleMetaData();
        Optional<ShardingRule> singleRule = ruleMetaData.findSingleRule(ShardingRule.class);
        if (singleRule.isEmpty()) {
            log.error("未找到对应的 ShardingRule 配置，动态分库分表更新失败 {}", singleRule);
            throw new NullPointerException("未找到对应的 ShardingRule 配置，动态分库分表更新失败");
        }
        RuleConfiguration configuration = singleRule.get().getConfiguration();
        ShardingRuleConfiguration ruleConfiguration = (ShardingRuleConfiguration) configuration;
        List<ShardingTableRuleConfiguration> collect = ruleConfiguration.getTables()
                .stream()
                .map(oldRole -> {
                    if (LOGIC_TABLE_NAME.equals(oldRole.getLogicTable())) {
                        ShardingTableRuleConfiguration newRole = new ShardingTableRuleConfiguration(LOGIC_TABLE_NAME, dataNodes);
                        newRole.setDatabaseShardingStrategy(oldRole.getDatabaseShardingStrategy());
                        newRole.setTableShardingStrategy(oldRole.getTableShardingStrategy());
                        newRole.setKeyGenerateStrategy(oldRole.getKeyGenerateStrategy());
                        newRole.setAuditStrategy(oldRole.getAuditStrategy());
                        return newRole;
                    }
                    return oldRole;
                })
                .collect(Collectors.toList());
        ruleConfiguration.setTables(collect);
        contextManager.alterRuleConfiguration(DATABASE_NAME, Collections.singleton(ruleConfiguration));
        contextManager.reloadDatabase(DATABASE_NAME);
        log.info("动态分库分表更新成功!");
    }

    /**
     * 获取对应的 ShardingSphere 和 ContextManager
     *
     * @return 上下文管理器
     */
    private ContextManager getContextManager() {
        try (ShardingSphereConnection connection = dataSource
                .getConnection()
                .unwrap(ShardingSphereConnection.class)) {
            return connection.getContextManager();
        } catch (SQLException ex) {
            log.error("获取 ShardingSphere ContextManager失败！ {}", ex.getMessage());
            throw new RuntimeException("获取 ShardingSphere ContextManager失败！", ex);
        }
    }

    /**
     * 为旗舰版用户单独进行分表
     *
     * @param space 空间对象
     */
    public void createSpacePictureTable(Space space) {
        // 动态创建分表
        // 仅为旗舰版团队空间创建分表
        MrsSpaceType spaceType = EnumUtil.fromValue(space.getSpaceType(), MrsSpaceType.class);
        SpaceLevelEnum spaceLevel = EnumUtil.fromValue(space.getSpaceLevel(), SpaceLevelEnum.class);
        if (spaceType == MrsSpaceType.TEAM && spaceLevel == SpaceLevelEnum.FLAGSHIP) {
            Long spaceId = space.getId();
            String tableName = "picture_" + spaceId;
            // 创建新表
            String createTableSql = "CREATE TABLE " + tableName + " LIKE picture";
            try {
                SqlRunner.db().update(createTableSql);
                // 更新分表
                updateShardingTableNodes();
            } catch (Exception e) {
                log.error("创建图片空间分表失败，空间 id = {}", space.getId());
            }
        }
    }

}
