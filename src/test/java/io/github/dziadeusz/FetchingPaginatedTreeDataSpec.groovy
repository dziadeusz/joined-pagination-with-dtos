package io.github.dziadeusz

import io.github.dziadeusz.tree.dto.BranchDto
import io.github.dziadeusz.tree.dto.LeafDto
import io.github.dziadeusz.tree.dto.TreeDto
import io.github.dziadeusz.tree.persistence.Branch
import io.github.dziadeusz.tree.persistence.Leaf
import io.github.dziadeusz.tree.persistence.TreeDao
import net.ttddyy.dsproxy.ConnectionInfo
import net.ttddyy.dsproxy.asserts.PreparedExecution
import net.ttddyy.dsproxy.asserts.ProxyTestDataSource
import net.ttddyy.dsproxy.proxy.ProxyConfig
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory
import net.ttddyy.dsproxy.proxy.SimpleResultSetProxyLogic
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder
import org.h2.jdbc.JdbcResultSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import sun.reflect.generics.tree.Tree

import javax.persistence.EntityManager
import javax.sql.DataSource
import java.sql.ResultSet

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.AUTO_CONFIGURED)
@ActiveProfiles("dbintegration")
@Transactional(propagation = NOT_SUPPORTED)
class FetchingPaginatedTreeDataSpec extends Specification {
    private static final int TOTAL_TREES = 10
    private static final int BRANCHES_PER_TREE = 2
    private static final int LEAFS_PER_BRANCH = 3

    @Autowired
    final TreeDao uut;
    @Autowired
    private ProxyTestDataSource proxyTestDataSource;
    private static int ROW_COUNT;

    void setup() {
        proxyTestDataSource.reset()
    }

    def "should get correctly paginated entities but all rows are fetched and pagination is in-memory"() {
        given:
        int page = 2;
        int size = 3;
        when:
        List<Tree> trees = uut.getTreeEntitiesWithInMemoryPagination(page, size)
        then:
        Set<Branch> branches = trees.branches.flatten()
        Set<Leaf> leafs = branches.leafs.flatten()
        trees.size() == size
        trees.name as Set == ['test tree 7', 'test tree 8', 'test tree 9'] as Set
        branches.size() == size * BRANCHES_PER_TREE
        leafs.size() == size * BRANCHES_PER_TREE * LEAFS_PER_BRANCH
        ROW_COUNT == TOTAL_TREES * BRANCHES_PER_TREE * LEAFS_PER_BRANCH
        List<PreparedExecution> executions = proxyTestDataSource.getQueryExecutions();
        executions.size() == 1
        def execution = executions.get(0);
        execution.getAllParameters().value.size() == 0
        def query = execution.query
        !query.contains("limit")
        !query.contains("offset")
    }

    def "should get incorrectly paginated trees with offset and limit applied to cross product"() {
        given:
        int page = 2;
        int size = 3;
        when:
        List<TreeDto> trees = uut.getTreesWithIncorrectPagination(page, size);
        then:
        Set<BranchDto> branches = trees.branches.flatten()
        Set<LeafDto> leafs = branches.leafs.flatten()
        trees.size() == 1
        branches.size() == 1
        leafs.size() == size
        ROW_COUNT == size
        leafs.name as Set == ['test leaf 7', 'test leaf 8', 'test leaf 9'] as Set
        List<PreparedExecution> executions = proxyTestDataSource.getQueryExecutions();
        executions.size() == 1
        def execution = executions.get(0);
        def limitAndOffset = execution.getAllParameters().value;
        limitAndOffset == [3, 6]
        execution.query.endsWith("limit ? offset ?")
    }

    def "should get corretly paginated trees with offset and limit applied to tree table"() {
        given:
        int page = 2;
        int size = 3;
        when:
        List<TreeDto> trees = uut.getTreesWithOffsetPagination(page, size);
        then:
        Set<BranchDto> branches = trees.branches.flatten()
        Set<LeafDto> leafs = branches.leafs.flatten()
        trees.size() == size
        trees.name as Set == ['test tree 7', 'test tree 8', 'test tree 9'] as Set
        branches.size() == size * BRANCHES_PER_TREE
        leafs.size() == size * BRANCHES_PER_TREE * LEAFS_PER_BRANCH
        ROW_COUNT == leafs.size()
        List<PreparedExecution> executions = proxyTestDataSource.getQueryExecutions();
        executions.size() == 1
        def execution = executions.get(0);
        def offsetAndLimit = execution.getAllParameters().value;
        offsetAndLimit == [6, 3]
    }

    @TestConfiguration
    static class BaseTestConfiguration {
        @Value("\${spring.datasource.url}")
        String url;
        @Value("\${spring.datasource.driver-class-name}")
        String driverClassName;

        @Bean
        TreeDao treeFacade(EntityManager entityManager) {
            return new TreeDao(entityManager);
        }

        @Bean
        DataSource testProxyDataSource() {
            final DataSource actualDataSource = DataSourceBuilder
                    .create()
                    .driverClassName(driverClassName)
                    .url(url)
                    .build()
            ResultSetProxyLogicFactory proxyFactory =
                    { ResultSet resultSet,
                      ConnectionInfo connectionInfo,
                      ProxyConfig proxyConfig ->
                        ROW_COUNT = ((JdbcResultSet) resultSet).result.rowCount;
                        return new SimpleResultSetProxyLogic(resultSet, connectionInfo, proxyConfig);
                    }
            def testDatasource = ProxyDataSourceBuilder.create(actualDataSource)
                    .countQuery()
                    .proxyResultSet(proxyFactory)
                    .build()

            new ProxyTestDataSource(testDatasource)
        }
    }
}
