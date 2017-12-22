package io.github.dziadeusz.tree.persistence;

import io.github.dziadeusz.tree.dto.TreeDto;
import org.hibernate.query.Query;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

public class TreeDao {

    private EntityManager entityManager;

    public TreeDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<Tree> getTreeEntitiesWithInMemoryPagination(final int page, final int size) {
        final int offset = page * size;
        return entityManager.createQuery(
                "SELECT t " +
                            "FROM Tree t " +
                            "LEFT JOIN FETCH t.branches b "+
                            "LEFT JOIN FETCH b.leafs l", Tree.class)
                .unwrap(Query.class)
                .setMaxResults(size)
                .setFirstResult(offset)
                .list();
    }

    @Transactional(readOnly = true)
    public List<TreeDto> getTreesWithIncorrectPagination(final int page, final int size) {
        final int offset = page * size;
        return entityManager.createNativeQuery(
                "SELECT " +
                            "t.id as TREE_ID, " +
                            "t.name as TREE_NAME, " +
                            "b.id as BRANCH_ID, " +
                            "b.name as BRANCH_NAME, " +
                            "l.id as LEAF_ID, " +
                            "l.name as LEAF_NAME " +
                        "FROM tree t " +
                            "LEFT OUTER JOIN branch b ON t.id = b.tree_id " +
                            "LEFT OUTER JOIN leaf l ON b.id = l.branch_id")
                .unwrap(Query.class)
                .setFirstResult(offset)
                .setMaxResults(size)
                .setResultTransformer(new TreeResultTransformer()).list();
    }

    @Transactional(readOnly = true)
    public List<TreeDto> getTreesWithOffsetPagination(final int page, final int size) {
        final int offset = page * size;
        return entityManager.createNativeQuery(
                "SELECT " +
                            "p_t.id as TREE_ID, " +
                            "p_t.name as TREE_NAME, " +
                            "b.id as BRANCH_ID, " +
                            "b.name as BRANCH_NAME, " +
                            "l.id as LEAF_ID, " +
                            "l.name as LEAF_NAME " +
                        "FROM " +
                            "(" +
                                "SELECT t.id, t.name " +
                                "FROM tree t " +
                                "OFFSET :treeOffset ROWS " +
                                "FETCH FIRST :treeLimit ROWS ONLY"+
                             ") p_t " +
                        "LEFT OUTER JOIN branch b ON P_t.id = b.tree_id "+
                        "LEFT OUTER JOIN leaf l ON b.id = l.branch_id")
                .unwrap(Query.class)
                .setParameter("treeLimit",size)
                .setParameter("treeOffset", offset)
                .setResultTransformer(new TreeResultTransformer()).list();
    }

}
