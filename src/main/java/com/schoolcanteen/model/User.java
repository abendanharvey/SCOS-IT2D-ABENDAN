package com.schoolcanteen.model;

public record User(
        int userId,
        String fullName,
        String username,
        String password,
        String role,
        String email,
        String accountStatus
) {
}