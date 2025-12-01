package com.xstream.clouddesktop.client.guacamole.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class UserPermissions {
    private Map<String, List<String>> connectionPermissions;
    private Map<String, List<String>> userPermissions;
}
