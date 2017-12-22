package io.github.dziadeusz.tree.persistence;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.persistence.*;
import java.util.Set;

@Table(name = "branch")
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
class Branch extends BaseEntity {

    String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_id")
    Tree tree;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "branch")
    Set<Leaf> leafs;

}
