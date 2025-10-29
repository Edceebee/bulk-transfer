package com.interswitch.bulktransaction.dto.request;

import java.util.List;

public record TokenRequest(String username, List<String> roles) {}

