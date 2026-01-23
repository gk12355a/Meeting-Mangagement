package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.room.RoomDTO;
import com.cmc.meeting.application.dto.room.RoomRequest;
import com.cmc.meeting.application.port.service.RoomService;
import com.cmc.meeting.domain.model.Room;
import com.cmc.meeting.domain.port.repository.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    private final com.cmc.meeting.application.port.storage.FileStoragePort fileStoragePort;

    public RoomServiceImpl(RoomRepository roomRepository, ModelMapper modelMapper,
            com.cmc.meeting.application.port.storage.FileStoragePort fileStoragePort) {
        this.roomRepository = roomRepository;
        this.modelMapper = modelMapper;
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(room -> modelMapper.map(room, RoomDTO.class))
                .collect(Collectors.toList());
    }

    // === MIGRATION LOGIC (US-33) ===
    @jakarta.annotation.PostConstruct
    public void migrateLocationData() {
        List<Room> rooms = roomRepository.findAll().stream()
                .map(entity -> modelMapper.map(entity, Room.class))
                .collect(Collectors.toList());

        for (Room room : rooms) {
            boolean changed = false;
            // Nếu chưa có floor/building nhưng có location -> Parse
            if (room.getFloor() == null && room.getLocation() != null && !room.getLocation().isEmpty()) {
                parseLocationToBuildingAndFloor(room);
                changed = true;
            }
            // Ngược lại, nếu có floor/building nhưng chưa có location -> Ghép (cho UI cũ)
            else if ((room.getLocation() == null || room.getLocation().isEmpty())
                    && room.getFloor() != null) {
                room.setLocation(buildLocationString(room.getBuildingName(), room.getFloor()));
                changed = true;
            }

            if (changed) {
                // Lưu lại (Repository nhận Domain Model)
                roomRepository.save(room);
            }
        }
    }

    private void parseLocationToBuildingAndFloor(Room room) {
        String loc = room.getLocation();
        // Regex 1: "Tầng 10" -> Floor 10
        // Regex 2: "Tầng 5 - Tòa C" -> Floor 5, Building "Tòa C"
        // Regex 3: "Tầng 3, Tòa nhà A" -> Floor 3, Building "Tòa nhà A"

        // Pattern: "Tầng" + khoảng trắng + (số) + (ký tự ngăn cách: " - " hoặc ", "
        // hoặc " – " hoặc " ") + (Building)
        // Đơn giản hóa: Tìm số sau chữ "Tầng", phần còn lại là tòa nhà.

        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("Tầng\\s+(\\d+)(.*)",
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(loc);

            if (m.find()) {
                String floorStr = m.group(1);
                String rest = m.group(2).trim();

                room.setFloor(Integer.parseInt(floorStr));

                // Cleanup "rest" để lấy Building Name
                // Remove leading separators like "- ", ", ", "– "
                if (!rest.isEmpty()) {
                    // Xóa ký tự đặc biệt ở đầu
                    String building = rest.replaceAll("^[-–,.]+\\s*", "").trim();
                    room.setBuildingName(building);
                }
            }
        } catch (Exception e) {
            // Log warning, không crash app
            System.err.println("Không thể parse location: " + loc);
        }
    }

    private String buildLocationString(String building, Integer floor) {
        if (floor == null)
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Tầng ").append(floor);
        if (building != null && !building.isEmpty()) {
            sb.append(" - ").append(building);
        }
        return sb.toString();
    }

    @Override
    public RoomDTO createRoom(RoomRequest request, List<org.springframework.web.multipart.MultipartFile> images) {
        // Manual Validation (từ khi disable annotation @NotBlank)
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên phòng không được để trống");
        }
        if (request.getCapacity() == null || request.getCapacity() < 1) {
            throw new IllegalArgumentException("Sức chứa phải ít nhất là 1");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }

        Room newRoom = modelMapper.map(request, Room.class);

        // Logic đồng bộ 2 chiều (nếu request thiếu 1 trong 2)
        if (newRoom.getFloor() != null) {
            // Có Floor -> Ưu tiên tạo Location từ Floor + Building
            newRoom.setLocation(buildLocationString(newRoom.getBuildingName(), newRoom.getFloor()));
        } else if (newRoom.getLocation() != null) {
            // Không có Floor nhưng có Location -> Parse ngược lại
            parseLocationToBuildingAndFloor(newRoom);
        }

        // Xử lý upload ảnh
        if (images != null && !images.isEmpty()) {
            java.util.List<String> imageUrls = new java.util.ArrayList<>();
            for (org.springframework.web.multipart.MultipartFile image : images) {
                if (!image.isEmpty()) {
                    try {
                        java.util.Map<String, String> result = fileStoragePort.uploadFile(image);
                        imageUrls.add(result.get("url"));
                    } catch (java.io.IOException e) {
                        throw new RuntimeException("Upload failed: " + e.getMessage());
                    }
                }
            }
            newRoom.setImages(imageUrls);
        }

        Room savedRoom = roomRepository.save(newRoom);
        return modelMapper.map(savedRoom, RoomDTO.class);
    }

    @Override
    public RoomDTO updateRoom(Long id, RoomRequest request,
            List<org.springframework.web.multipart.MultipartFile> images) {
        Room existingRoom = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng: " + id));

        // Cập nhật các trường cơ bản
        // Cập nhật các trường cơ bản (PATCH STYLE - Chỉ cập nhật nếu không null)
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            existingRoom.setName(request.getName());
        }
        if (request.getCapacity() != null && request.getCapacity() > 0) {
            existingRoom.setCapacity(request.getCapacity());
        }
        if (request.getFixedDevices() != null) {
            existingRoom.setFixedDevices(request.getFixedDevices());
        }
        if (request.getRequiredRoles() != null) {
            existingRoom.setRequiredRoles(request.getRequiredRoles());
        }
        if (request.getStatus() != null) {
            existingRoom.setStatus(request.getStatus());
        }
        if (request.getRequiresApproval() != null) {
            existingRoom.setRequiresApproval(request.getRequiresApproval());
        }

        // Cập nhật Location / Building / Floor
        // Logic: Ưu tiên Building/Floor từ request nếu có
        if (request.getFloor() != null) {
            existingRoom.setFloor(request.getFloor());
            existingRoom.setBuildingName(request.getBuildingName());
            // Sync lại location
            existingRoom.setLocation(buildLocationString(request.getBuildingName(), request.getFloor()));
        }
        // Nếu không gửi Building/Floor, check xem có gửi Location mới không
        if (request.getLocation() != null && !request.getLocation().equals(existingRoom.getLocation())) {
            existingRoom.setLocation(request.getLocation());
            // Parse ngược lại
            parseLocationToBuildingAndFloor(existingRoom);
        }

        // Xử lý xóa ảnh (nếu có yêu cầu)
        if (request.getDeleteImages() != null && !request.getDeleteImages().isEmpty()) {
            if (existingRoom.getImages() != null) {
                existingRoom.getImages().removeAll(request.getDeleteImages());

                // Optional: Gọi Cloudinary để xóa file trên cloud luôn nếu muốn tiết kiệm
                // storage
                // for (String imgUrl : request.getDeleteImages()) {
                // fileStoragePort.deleteFile(imgUrl);
                // }
            }
        }

        // Xử lý upload ảnh (cộng dồn)
        if (images != null && !images.isEmpty()) {
            if (existingRoom.getImages() == null) {
                existingRoom.setImages(new java.util.ArrayList<>());
            }
            for (org.springframework.web.multipart.MultipartFile image : images) {
                if (!image.isEmpty()) {
                    try {
                        java.util.Map<String, String> result = fileStoragePort.uploadFile(image);
                        existingRoom.getImages().add(result.get("url"));
                    } catch (java.io.IOException e) {
                        throw new RuntimeException("Upload failed: " + e.getMessage());
                    }
                }
            }
        }

        Room updatedRoom = roomRepository.save(existingRoom);
        return modelMapper.map(updatedRoom, RoomDTO.class);
    }

    // ... keep deleteRoom and findAvailableRooms
    @Override
    public void deleteRoom(Long id) {
        if (!roomRepository.findById(id).isPresent()) {
            throw new EntityNotFoundException("Không tìm thấy phòng: " + id);
        }
        roomRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> findAvailableRooms(LocalDateTime startTime, LocalDateTime endTime, int capacity) {
        List<Room> availableRooms = roomRepository.findAvailableRooms(startTime, endTime, capacity);
        return availableRooms.stream()
                .map(room -> modelMapper.map(room, RoomDTO.class))
                .collect(Collectors.toList());
    }
}