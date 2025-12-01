package com.xstream.clouddesktop.dto.response;

import com.xstream.clouddesktop.model.Desktop;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class DesktopListResponse {
    private List<DesktopResponse> desktops;
    private Integer total;

    public static DesktopListResponse fromEntities(List<Desktop> desktops) {
        return DesktopListResponse.builder()
                .desktops(desktops.stream().map(DesktopResponse::fromEntity).collect(Collectors.toList()))
                .total(desktops.size())
                .build();
    }
}
