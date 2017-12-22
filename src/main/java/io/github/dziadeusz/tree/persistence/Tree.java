package io.github.dziadeusz.tree.persistence;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "tree")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
class Tree extends BaseEntity {

    String name;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "tree")
    Set<Branch> branches;
}
