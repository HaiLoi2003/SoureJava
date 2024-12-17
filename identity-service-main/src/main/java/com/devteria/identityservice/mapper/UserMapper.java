package com.devteria.identityservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.devteria.identityservice.dto.request.UserCreationRequest;
import com.devteria.identityservice.dto.request.UserUpdateRequest;
import com.devteria.identityservice.dto.response.UserResponse;
import com.devteria.identityservice.entity.User;

@Mapper(componentModel = "spring") // Đánh dấu interface này là mapper, MapStruct sẽ tạo ra implementation của nó. "spring" giúp Spring Boot quản lý mapper này như một bean.
public interface UserMapper {

    // Chuyển từ UserCreationRequest sang đối tượng User
    User toUser(UserCreationRequest request);

    // Chuyển từ User sang đối tượng phản hồi UserResponse
    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true) // Bỏ qua thuộc tính "roles" khi cập nhật User từ UserUpdateRequest
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
    // Phương thức này sẽ cập nhật đối tượng `User` hiện có với các thuộc tính từ `UserUpdateRequest`
}
