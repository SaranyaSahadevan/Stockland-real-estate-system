package com.stockland.app.controller;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.dto.UserResponseDTO;
import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import com.stockland.app.service.PropertyService;
import com.stockland.app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/properties")
public class PropertyController {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public String viewProperty(@PathVariable Long id, Model model) {
        PropertyResponseDTO property = propertyService.findById(id);
        model.addAttribute("property", property);
        return "property";
    }

    @PostMapping("/delete/{id}")
    public String deleteProperty(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        PropertyResponseDTO property = propertyService.findById(id);
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && !property.getUsername().equals(userDetails.getUsername())) {
            request.setAttribute("jakarta.servlet.error.status_code", 403);
            request.setAttribute("errorMessage", "You do not have permission to delete this listing.");
            request.getRequestDispatcher("/error").forward(request, response);
            return null;
        }
        propertyService.deleteById(id);
        return "redirect:/dashboard?deleted";
    }

    @GetMapping("/edit/{id}")
    public String editPropertyForm(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam(value = "redirectUrl", defaultValue = "/dashboard") String redirectUrl,
                                   Model model,
                                   HttpServletRequest request,
                                   HttpServletResponse response) throws Exception {
        PropertyResponseDTO property = propertyService.findById(id);
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && !property.getUsername().equals(userDetails.getUsername())) {
            request.setAttribute("jakarta.servlet.error.status_code", 403);
            request.setAttribute("errorMessage", "You do not have permission to edit this listing.");
            request.getRequestDispatcher("/error").forward(request, response);
            return null;
        }

        PropertyRequestDTO dto = PropertyRequestDTO.builder()
                .id(property.getId())
                .title(property.getTitle())
                .location(property.getLocation())
                .price(property.getPrice() != null ? String.format("%.2f", property.getPrice()).replace(".", ",") : "")
                .description(property.getDescription())
                .actionType(property.getActionType())
                .propertyType(property.getPropertyType())
                .status(property.getStatus())
                .area(property.getArea())
                .roomCount(property.getRoomCount())
                .build();

        String[] images = property.getImages();

        model.addAttribute("propertyRequestDTO", dto);
        model.addAttribute("images", images);
        model.addAttribute("actions", ActionType.values());
        model.addAttribute("propertyTypes", PropertyType.values());
        model.addAttribute("redirectUrl", redirectUrl);
        return "edit-listing";
    }

    @PostMapping("/edit/{id}")
    public String editProperty(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               @Valid PropertyRequestDTO propertyRequestDTO,
                               BindingResult bindingResult,
                               @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
                               @RequestParam(value = "deleteImageIds", required = false) List<String> imageUrlsToDelete,
                               @RequestParam(value = "redirectUrl", defaultValue = "/dashboard") String redirectUrl,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               Model model) throws Exception {

        PropertyResponseDTO property = propertyService.findById(id);
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && !property.getUsername().equals(userDetails.getUsername())) {
            request.setAttribute("jakarta.servlet.error.status_code", 403);
            request.setAttribute("errorMessage", "You do not have permission to edit this listing.");
            request.getRequestDispatcher("/error").forward(request, response);
            return null;
        }

        if (bindingResult.hasErrors()) {
            propertyRequestDTO.setId(property.getId());

            model.addAttribute("propertyRequestDTO", propertyRequestDTO);
            model.addAttribute("images", property.getImages());
            model.addAttribute("actions", ActionType.values());
            model.addAttribute("propertyTypes", PropertyType.values());
            model.addAttribute("redirectUrl", redirectUrl);
            return "edit-listing";
        }

        propertyService.updateProperty(id, propertyRequestDTO, imageFiles, imageUrlsToDelete, isAdmin);
        return "redirect:" + redirectUrl + (redirectUrl.contains("?") ? "&updated" : "?updated");
    }

    @PostMapping("/create")
    public String createProperty(@AuthenticationPrincipal UserDetails userDetails,
                                 @Valid PropertyRequestDTO propertyRequestDTO,
                                 BindingResult bindingResult,
                                 @RequestParam(value = "imageFiles" , required = false) MultipartFile[] imageFiles,
                                 Model model){
        if (bindingResult.hasErrors()) {
            model.addAttribute("actions", ActionType.values());
            model.addAttribute("propertyTypes", PropertyType.values());
            model.addAttribute("propertyRequestDTO", propertyRequestDTO);
            return "create-listing";
        }

        String username = userDetails.getUsername();
        UserResponseDTO user = userService.findByUsername(username);

        propertyService.saveProperty(propertyRequestDTO, user.getId(), imageFiles);

        if (!userService.usernameExists(username)) {
            throw new RuntimeException("Provided username does not exist when creating a new property: " + username);
        }

        model.addAttribute("success", "Property listing created successfully!");
        return "redirect:/dashboard";
    }

    @PostMapping("/approve/{id}")
    public String approveProperty(@PathVariable Long id,
                                  @RequestParam(value = "redirectUrl", defaultValue = "/dashboard?approved") String redirectUrl) {
        propertyService.approveProperty(id);
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/reject/{id}")
    public String rejectProperty(@PathVariable Long id,
                                 @RequestParam(value = "redirectUrl", defaultValue = "/dashboard?rejected") String redirectUrl) {
        propertyService.rejectProperty(id);
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/feature/{id}")
    public String toggleFeatured(@PathVariable Long id) {
        propertyService.toggleFeatured(id);
        return "redirect:/dashboard#admin-panel";
    }
}