package com.rygf.controller;

import com.rygf.common.ImageUploader;
import com.rygf.dto.CrudStatus;
import com.rygf.dto.CrudStatus.STATUS;
import com.rygf.dto.PostDTO;
import com.rygf.entity.Post;
import com.rygf.entity.Subject;
import com.rygf.exception.ImageException;
import com.rygf.service.PostService;
import com.rygf.service.SubjectService;
import java.util.List;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@AllArgsConstructor
@Slf4j
//...
@RequestMapping("/dashboard/post")
@Controller
public class PostController {
    
    private PostService postService;
    private SubjectService subjectService;
    private ImageUploader imageUploader;
    
    @ModelAttribute("crudStatus")
    public CrudStatus getCrudStatus() {
        return new CrudStatus();
    }
    
    @ModelAttribute("subjects")
    public List<Subject> getSubjects() {
        return subjectService.findAll();
    }
    
    @PreAuthorize("hasAuthority('POST_READ')")
    @GetMapping
    public String showPostDashboard(Model model) {
        List<Post> posts = postService.findAll();
        model.addAttribute("posts", posts);
        
        return "post/dashboard";
    }
    
    @PreAuthorize("hasAuthority('POST_CREATE')")
    @GetMapping("/create")
    public String showPostForm(@ModelAttribute("post")PostDTO postDTO) {
        return "post/form";
    }
    
    @PreAuthorize("hasAnyAuthority('POST_CREATE', 'POST_UDPATE')")
    @PostMapping("/submit")
    public String processForm(@Valid @ModelAttribute("post")PostDTO postDTO,
        BindingResult rs,
        RedirectAttributes ra
    ) {
        if(rs.hasErrors()) {
            return "post/form";
        }
    
        MultipartFile source = postDTO.getThumbnail();
        try {
            if(postDTO.getId() == null && (source == null || source.isEmpty()))
                throw new ImageException("ERR_UPLOAD_IMAGE_NULL");
            else if(postDTO.getId() != null && (source == null || source.isEmpty())) {
                // là trường hợp update nhưng không update Thumbnail
            } else {
                if(postDTO.getId() != null) // Xóa exists thumbnail
                    postService.deleteExistThumbnail(postDTO.getId());
                String finalDesFileName = imageUploader.uploadFile(source);
                postDTO.setFinalDesFileName(finalDesFileName);
            }
            
        } catch (ImageException e) {
            rs.rejectValue("thumbnail", null, e.getMessage());
            return "post/form";
        }
        
        postService.createOrUpdate(postDTO);
        if(postDTO.getId() == null)
            ra.addFlashAttribute("crudStatus", new CrudStatus(STATUS.CREATE_SUCCESS));
        else if(postDTO.getId() != null)
            ra.addFlashAttribute("crudStatus", new CrudStatus(STATUS.UPDATE_SUCCESS));
        return "redirect:/dashboard/post";
    }
    
    @PreAuthorize("hasAuthority('POST_UPDATE')")
    @GetMapping("/{id}/update")
    public String showUpdatePage(@PathVariable("id")Long id,
            Model model
        ) {
        PostDTO post = postService.findDto(id);
        model.addAttribute("post", post);
        return "post/form";
    }
    
    @PreAuthorize("hasAuthority('POST_DELETE')")
    @GetMapping("/{id}/delete")
    public String processDelete(@PathVariable("id")Long id, RedirectAttributes ra) {
        postService.delete(id);
        
        ra.addFlashAttribute("crudStatus", new CrudStatus(STATUS.DELETE_SUCCESS));
        return "redirect:/dashboard/post";
    }
    
}
