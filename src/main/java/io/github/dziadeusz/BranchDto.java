package io.github.dziadeusz;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Builder
@Value
@EqualsAndHashCode(of = {"id"})
public class BranchDto {
    private Long id;
    private String name;
    private Set<LeafDto> leafs = new HashSet<>();

    public void addLeaf(LeafDto leaf) {
        Objects.requireNonNull(leaf);
        leafs.add(leaf);
    }
}
