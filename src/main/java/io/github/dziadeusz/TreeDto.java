package io.github.dziadeusz;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Builder
@Value
@EqualsAndHashCode(of = {"id"})
public class TreeDto {

    private Long id;
    private String name;
    private Set<BranchDto> branches = new LinkedHashSet<>();

    public void addBranch(BranchDto branch) {
        Objects.requireNonNull(branch);
        this.branches.add(branch);
    }
}
