package io.github.dziadeusz

import net.ttddyy.dsproxy.asserts.ProxyTestDataSource
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
    TreeDao uut;

    def "should get incorrectly paginated trees with offset and limit applied to cross product"() {
        given:
        int page = 1;
        int size = 4;
        when:
        List<TreeDto> trees = uut.getTreesWithIncorrectPagination(page, size);
        then:
        Set<BranchDto> branches = trees.branches.flatten()
        Set<LeafDto> leafs = branches.leafs.flatten()
        trees.size()==2
        branches.size() == 2
        leafs.size()== size
        leafs.name as Set == (page * size + 1..page * size + size).collect {it -> 'test leaf ' + it} as Set
    }

    def "should get corretly paginated trees with offset and limit applied to tree table"() {
        given:
        int page = 1;
        int size = 4;
        when:
        List<TreeDto> trees = uut.getTreesWithOffsetPagination(page, size);
        then:
        Set<BranchDto> branches = trees.branches.flatten()
        Set<LeafDto> leafs = branches.leafs.flatten()
        trees.size()== size
        trees.name as Set == ['test tree 5', 'test tree 6', 'test tree 7', 'test tree 8'] as Set
        branches.size() == size * BRANCHES_PER_TREE
        leafs.size()== size * BRANCHES_PER_TREE * LEAFS_PER_BRANCH
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
