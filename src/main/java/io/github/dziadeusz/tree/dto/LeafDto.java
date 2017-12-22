package io.github.dziadeusz.tree.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Set;

@Builder
@Value
@EqualsAndHashCode(of = {"id"})
public class LeafDto {

    private Long id;
    private String name;

}
