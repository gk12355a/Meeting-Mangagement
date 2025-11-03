package com.cmc.meeting.application.service;

import com.cmc.meeting.application.dto.attachment.AttachmentDTO;
import com.cmc.meeting.application.port.service.AttachmentService;
import com.cmc.meeting.application.port.storage.FileStoragePort;
import com.cmc.meeting.domain.exception.PolicyViolationException;
import com.cmc.meeting.domain.model.Meeting;
import com.cmc.meeting.domain.model.MeetingAttachment;
import com.cmc.meeting.domain.port.repository.MeetingAttachmentRepository;
import com.cmc.meeting.domain.port.repository.MeetingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Transactional
public class AttachmentServiceImpl implements AttachmentService {

    private final FileStoragePort fileStorage; // Adapter Cloudinary
    private final MeetingRepository meetingRepository;
    private final MeetingAttachmentRepository attachmentRepository;
    private final ModelMapper modelMapper;

    public AttachmentServiceImpl(FileStoragePort fileStorage, 
                                 MeetingRepository meetingRepository, 
                                 MeetingAttachmentRepository attachmentRepository, 
                                 ModelMapper modelMapper) {
        this.fileStorage = fileStorage;
        this.meetingRepository = meetingRepository;
        this.attachmentRepository = attachmentRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public AttachmentDTO uploadAttachment(Long meetingId, Long currentUserId, MultipartFile file) throws IOException {
        // 1. Kiểm tra quyền
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cuộc họp"));

        if (!meeting.getOrganizer().getId().equals(currentUserId)) {
            throw new PolicyViolationException("Chỉ người tổ chức mới có quyền đính kèm file.");
        }

        // 2. Upload file lên Cloudinary
        Map<String, String> uploadResult = fileStorage.uploadFile(file);
        String fileUrl = uploadResult.get("url");
        String publicId = uploadResult.get("public_id");

        // 3. Lưu thông tin vào CSDL
        MeetingAttachment attachment = new MeetingAttachment(
                file.getOriginalFilename(),
                fileUrl,
                publicId,
                meeting
        );
        MeetingAttachment savedAttachment = attachmentRepository.save(attachment);

        return modelMapper.map(savedAttachment, AttachmentDTO.class);
    }

    @Override
    public void deleteAttachment(Long attachmentId, Long currentUserId) throws IOException {
        // 1. Tìm attachment
        MeetingAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy file đính kèm"));

        // 2. Kiểm tra quyền
        if (!attachment.getMeeting().getOrganizer().getId().equals(currentUserId)) {
            throw new PolicyViolationException("Chỉ người tổ chức mới có quyền xóa file này.");
        }

        // 3. Xóa file trên Cloudinary
        fileStorage.deleteFile(attachment.getFilePublicId());

        // 4. Xóa file trong CSDL
        attachmentRepository.delete(attachment);
    }
}