package io.github.dziadeusz.tree.persistence;

import io.github.dziadeusz.tree.dto.BranchDto;
import io.github.dziadeusz.tree.dto.LeafDto;
import io.github.dziadeusz.tree.dto.TreeDto;
import org.hibernate.transform.BasicTransformerAdapter;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class TreeResultTransformer extends BasicTransformerAdapter {

    private static final String TREE_ID_COLUMN = "TREE_ID";
    private static final String TREE_NAME_COLUMN = "TREE_NAME";
    private static final String BRANCH_ID_COLUMN = "BRANCH_ID";
    private static final String BRANCH_NAME_COLUMN = "BRANCH_NAME";
    private static final String LEAF_ID_COLUMN = "LEAF_ID";
    private static final String LEAF_NAME_COLUMN = "LEAF_NAME";

    private final Map<Long, TreeDto> trees = new HashMap<>();
    private final Map<Long, BranchDto> branches = new HashMap<>();

    @Override
    public Object transformTuple(final Object[] tuple, final String[] aliases) {
        final List<String> aliasesList = Arrays.asList(aliases);
        final TreeDto tree = getTreeOrAddNew(tuple, aliasesList);
        final BranchDto branch = getBranchOrAddNew(tuple, aliasesList, tree);
        addNewLeafToBranch(tuple, aliasesList, branch);
        return tree;
    }

    private void addNewLeafToBranch(final Object[] tuple, final List<String> aliasesList, final BranchDto branch) {
        final BigInteger leafIdToParse = (BigInteger) tuple[aliasesList.indexOf(LEAF_ID_COLUMN)];
        final Long leafId = leafIdToParse != null ? leafIdToParse.longValue() : null;
        if (leafId != null) {
            final String leafName = (String) tuple[aliasesList.indexOf(LEAF_NAME_COLUMN)];
            branch.addLeaf(LeafDto.builder().id(leafId).name(leafName).build());
        }
    }

    private BranchDto getBranchOrAddNew(final Object[] tuple, final List<String> aliasesList, final TreeDto tree) {
        BranchDto branch = null;
        final BigInteger branchIdToParse = (BigInteger) tuple[aliasesList.indexOf(BRANCH_ID_COLUMN)];
        final Long branchId = branchIdToParse != null ? branchIdToParse.longValue() : null;
        if (branchId != null) {
            if (!branches.containsKey(branchId)) {
                final String branchName = (String) tuple[aliasesList.indexOf(BRANCH_NAME_COLUMN)];
                branch = BranchDto.builder().id(branchId).name(branchName).build();
                tree.addBranch(branch);
                branches.put(branchId, branch);
            } else {
                branch = branches.get(branchId);
            }

        }
        return branch;
    }

    private TreeDto getTreeOrAddNew(final Object[] tuple, final List<String> aliasesList) {
        final Long treeId = ((BigInteger) tuple[aliasesList.indexOf(TREE_ID_COLUMN)]).longValue();
        Objects.requireNonNull(treeId);
        final TreeDto tree;
        if (!trees.containsKey(treeId)) {
            final String treeName = (String) tuple[aliasesList.indexOf(TREE_NAME_COLUMN)];
            tree = TreeDto.builder().id(treeId).name(treeName).build();
            trees.put(treeId, tree);
        } else {
            tree = trees.get(treeId);
        }
        return tree;
    }

    @Override
    public List transformList(final List records) {
        return (List) records.stream().distinct().collect(Collectors.toList());
    }
}
