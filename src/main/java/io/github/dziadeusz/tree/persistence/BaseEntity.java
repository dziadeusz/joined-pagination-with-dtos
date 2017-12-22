package io.github.dziadeusz.tree.persistence;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import javax.persistence.*;
import java.util.UUID;

@MappedSuperclass
@Getter
@EqualsAndHashCode(of="id")
@FieldDefaults(level = AccessLevel.PRIVATE)
class BaseEntity {

    @Id
    Long id;
}
