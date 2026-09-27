package com.example.service;

import com.example.entity.Menu;
import com.example.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    /**
     * Lấy thông tin menu theo ID.
     * Sử dụng @Cacheable để lưu kết quả vào cache "menuCache".
     */
    @Cacheable(value = "menuCache", key = "#id")
    public Menu getMenuById(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với ID: " + id));
    }

    /**
     * Cập nhật thông tin món ăn và xóa cache tương ứng.
     * Áp dụng chiến lược Cache-Aside: Cập nhật DB trước, xóa cache sau.
     * 
     * @param menu Đối tượng Menu chứa thông tin cập nhật
     * @return Menu đã được cập nhật trong DB
     */
    @Transactional
    @CacheEvict(value = "menuCache", key = "#menu.id")
    public Menu updateMenu(Menu menu) {
        // Bước 1: Thực hiện cập nhật database trước
        Menu updatedMenu = menuRepository.save(menu);
        
        // Bước 2: @CacheEvict sẽ tự động xóa key tương ứng trong "menuCache" sau khi phương thức thực thi thành công
        return updatedMenu;
    }
}

/* 
===================================================================================
GIẢI THÍCH: VÌ SAO CHỌN @CacheEvict THAY VÌ @CachePut ĐỂ TRÁNH RACE CONDITION
===================================================================================

1. Vấn đề Race Condition với @CachePut:
   - Trong môi trường xử lý đồng thời (Concurrent Requests), giả sử có 2 request cập nhật (Request A và Request B) cùng chỉnh sửa một món ăn:
     + Request A cập nhật DB với giá 70.000đ.
     + Request B cập nhật DB với giá 75.000đ.
   - Do độ trễ mạng hoặc tiến trình xử lý, thứ tự ghi vào Cache có thể bị đảo ngược:
     + Request B ghi vào Cache trước (giá 75.000đ).
     + Request A ghi vào Cache sau (giá 70.000đ).
   - Kết quả: DB lưu giá mới nhất là 75.000đ, nhưng Cache lại lưu giá cũ là 70.000đ. Dữ liệu bị không nhất quán (Stale Data) kéo dài cho đến khi cache hết hạn.

2. Giải pháp với @CacheEvict (Chiến lược Cache-Aside):
   - Thay vì cố gắng ghi đè dữ liệu mới vào Cache (@CachePut), ta thực hiện xóa Cache (@CacheEvict).
   - Khi request GET tiếp theo được gọi, hệ thống thấy Cache bị trống (Cache Miss) nên sẽ truy vấn dữ liệu chuẩn từ DB và nạp lại vào Cache.
   - Điều này đảm bảo tính nhất quán dữ liệu (Data Consistency) và tránh được hoàn toàn viễn cảnh Race Condition khi ghi cache.
===================================================================================
*/