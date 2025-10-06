package com.assessment.quest.part1.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class BLSFileMetadata implements FileMetadata {

    private String name;

    private long size;

    private long lastModified;
}
