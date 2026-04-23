package com.nutalig.controller.file;

import com.nutalig.controller.file.response.UploadImageResponse;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/files")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GeneralResponse uploadImage(@RequestPart("file") MultipartFile file) throws Exception {
        log.info("Upload image {}", file.getOriginalFilename());

        UploadImageResponse response = fileStorageService.uploadImage(file);

        return new GeneralResponse<>(SUCCESS, response);
    }
}
