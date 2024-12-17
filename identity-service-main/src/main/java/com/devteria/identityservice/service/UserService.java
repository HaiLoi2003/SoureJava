package com.devteria.identityservice.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.devteria.identityservice.constant.PredefinedRole;
import com.devteria.identityservice.dto.request.UserCreationRequest;
import com.devteria.identityservice.dto.request.UserUpdateRequest;
import com.devteria.identityservice.dto.response.UserResponse;
import com.devteria.identityservice.entity.Role;
import com.devteria.identityservice.entity.User;
import com.devteria.identityservice.exception.AppException;
import com.devteria.identityservice.exception.ErrorCode;
import com.devteria.identityservice.mapper.UserMapper;
import com.devteria.identityservice.repository.RoleRepository;
import com.devteria.identityservice.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service // Đánh dấu class là một service của Spring
@RequiredArgsConstructor // Tự động tạo constructor cho các trường final, giúp injection tự động
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // Đặt các biến ở private final và cố định
@Slf4j // Để hỗ trợ ghi log
public class UserService {
    UserRepository userRepository; // Repository để truy cập dữ liệu người dùng
    RoleRepository roleRepository; // Repository để truy cập dữ liệu vai trò
    UserMapper userMapper; // Mapper để chuyển đổi dữ liệu giữa các lớp đối tượng và DTO
    PasswordEncoder passwordEncoder; // Mã hóa mật khẩu người dùng

    // Phương thức tạo người dùng mới
    public UserResponse createUser(UserCreationRequest request) {
        // Chuyển đổi yêu cầu người dùng thành đối tượng User
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Mã hóa mật khẩu

        // Tạo tập hợp vai trò và thêm vai trò "USER" mặc định
        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(PredefinedRole.USER_ROLE).ifPresent(roles::add);
        user.setRoles(roles); // Gán vai trò cho người dùng

        try {
            user = userRepository.save(user); // Lưu người dùng vào cơ sở dữ liệu
        } catch (DataIntegrityViolationException exception) { // Xử lý lỗi nếu người dùng đã tồn tại
            throw new AppException(ErrorCode.USER_EXISTED); // Ném ngoại lệ với mã lỗi
        }

        return userMapper.toUserResponse(user); // Trả về phản hồi người dùng dưới dạng DTO
    }

    // Phương thức lấy thông tin của người dùng hiện tại
    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext(); // Lấy ngữ cảnh bảo mật
        String name = context.getAuthentication().getName(); // Lấy tên người dùng từ ngữ cảnh

        // Tìm người dùng theo tên, nếu không thấy thì ném ngoại lệ
        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user); // Trả về phản hồi người dùng dưới dạng DTO
    }

    @PostAuthorize("returnObject.username == authentication.name") // Chỉ cho phép nếu người dùng cập nhật thông tin của chính mình
    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        // Tìm người dùng theo ID, nếu không thấy thì ném ngoại lệ
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Cập nhật thông tin người dùng từ request
        userMapper.updateUser(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Mã hóa mật khẩu mới

        // Tìm tất cả các vai trò trong request và gán cho người dùng
        var roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));

        return userMapper.toUserResponse(userRepository.save(user)); // Lưu và trả về thông tin người dùng đã cập nhật
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép admin xóa người dùng
    public void deleteUser(String userId) {
        userRepository.deleteById(userId); // Xóa người dùng theo ID
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép admin truy cập danh sách người dùng
    public List<UserResponse> getUsers() {
        log.info("In method get Users"); // Ghi log
        // Lấy tất cả người dùng từ DB và chuyển đổi sang DTO
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép admin truy cập thông tin người dùng
    public UserResponse getUser(String id) {
        // Tìm người dùng theo ID và trả về DTO, nếu không thấy thì ném ngoại lệ
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }
}
