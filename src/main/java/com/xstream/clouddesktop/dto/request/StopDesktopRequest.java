package com.xstream.clouddesktop.dto.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopDesktopRequest {
    @Builder.Default
    private boolean force = false;
}
