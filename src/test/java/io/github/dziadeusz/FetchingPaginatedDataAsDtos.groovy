package io.github.dziadeusz

import io.github.dziadeusz.tree.persistence.TreeDao
import io.github.dziadeusz.tree.dto.BranchDto
import io.github.dziadeusz.tree.dto.LeafDto
import io.github.dziadeusz.tree.dto.TreeDto
import net.ttddyy.dsproxy.asserts.ParameterKeyValue
import net.ttddyy.dsproxy.asserts.PreparedExecution
import net.ttddyy.dsproxy.asserts.ProxyTestDataSource
import net.ttddyy.dsproxy.asserts.QueryExecution
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder
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

import javax.persistence.EntityManager
import javax.sql.DataSource

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.AUTO_CONFIGURED)
@ActiveProfiles("dbintegration")
@Transactional(propagation = NOT_SUPPORTED)
class FetchingPaginatedDataAsDtos extends Specification {

    private static final int BRANCHES_PER_TREE = 2
    private static final int LEAFS_PER_BRANCH = 3

    @Autowired
    final TreeDao uut;
    @Autowired
    private ProxyTestDataSource proxyTestDataSource;

    void setup() {
        proxyTestDataSource.reset()
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
        trees.size()==1
        branches.size() == 1
        leafs.size()== size
        leafs.name as Set == ['test leaf 7', 'test leaf 8', 'test leaf 9'] as Set
        List<PreparedExecution> executions = proxyTestDataSource.getQueryExecutions();
        executions.size() == 1
        def execution = executions.get(0);
        def limitAndOffset = execution.getAllParameters().value;
        limitAndOffset == [3, 6]
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
        trees.size()== size
        trees.name as Set == ['test tree 7', 'test tree 8', 'test tree 9'] as Set
        branches.size() == size * BRANCHES_PER_TREE
        leafs.size()== size * BRANCHES_PER_TREE * LEAFS_PER_BRANCH
        List<PreparedExecution> executions = proxyTestDataSource.getQueryExecutions();
        executions.size() == 1
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
            def testDatasource = ProxyDataSourceBuilder.create(actualDataSource)
                    .countQuery().build()
            new ProxyTestDataSource(testDatasource)
        }
    }
}
