package mes.app.system;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import mes.app.UtilClass;
import mes.domain.entity.MenuItem;
import mes.domain.entity.UserCode;
import mes.domain.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mes.app.system.service.UserGroupService;
//import mes.app.system.usergroup.service.UserGroupService;
import mes.domain.entity.User;
import mes.domain.entity.UserGroup;
import mes.domain.model.AjaxResult;
import mes.domain.repository.UserGroupRepository;

@RestController
@RequestMapping("/api/system/usergroup")
public class UserGroupController{
	
	@Autowired
	UserGroupRepository userGroupRepository;
	
	@Autowired 
	private UserGroupService userGroupService;

    @Autowired
    private MenuItemRepository menuItemRepository;


	@GetMapping("/read")
	public AjaxResult getUserGroupList(HttpServletRequest request,
			Authentication auth) {
		
		User user = (User)auth.getPrincipal();
		Boolean super_user = user.getSuperUser();
		
		if (super_user == false ) {
			super_user = user.getUserProfile().getUserGroup().getCode() == "dev" ;
		}			
				
		
		List<Map<String,Object>> items = this.userGroupService.getUserGroupList(super_user);
		AjaxResult result = new AjaxResult();
		result.data = items;
		return result;
		
	}
	
	@GetMapping("/defaultmenu")
	public AjaxResult getUserGroup() {

		SecurityContext sc = SecurityContextHolder.getContext();
		Authentication auth = sc.getAuthentication();
		User user = (User)auth.getPrincipal();

		Map<String, Object> item = this.userGroupService.getDefaultMenu(user);
		
		AjaxResult result = new AjaxResult();
		result.data = item;
		return result;
	}

	@GetMapping("/detail")
	public AjaxResult getUserGroup(@RequestParam("id") int id) {

		Map<String, Object> item = this.userGroupService.getUserGroup(id);

		AjaxResult result = new AjaxResult();
		result.data = item;
		return result;
	}

	@PostMapping("/save")
	public AjaxResult saveUserGroup(
			@RequestParam(value="id", required= false) Integer id,
			@RequestParam("code") String code,
			@RequestParam("name") String name,
			@RequestParam ("description") String description,
			@RequestParam("gmenu") String gmenu,
			HttpServletRequest request,
			Authentication auth) {
		User user = (User)auth.getPrincipal();

		UserGroup ug=null;
		AjaxResult result = new AjaxResult();

		if(id == null) {
			ug = new UserGroup();

			UserGroup byCode = userGroupRepository.findByCode(code);
			if(byCode != null){
				result.success = false;
				result.message = "중복된 코드가 존재합니다.";
				return result;
			}

		} else {
			ug = this.userGroupRepository.getUserGrpById(id);
		}

		ug.setName(name);
		ug.setCode(code);
		ug.setDescription(description);
		ug.set_audit(user);
		ug.setGmenu(gmenu);

		ug = this.userGroupRepository.save(ug);


		result.data = ug;

		result.success = true;
		result.message = "저장완료하였습니다.";
		return result;

	}


	@PostMapping("/delete")
	public AjaxResult deleteUserGroup(@RequestParam("id") String id) {

		List<Integer> paramList = new UtilClass().parseUserIdsToInt(id);

		for(Integer param : paramList){
			this.userGroupRepository.deleteById(param);
		}

		AjaxResult result = new AjaxResult();
		result.success = true;
		result.message = "삭제하였습니다.";
		return result;
	}

	/*
	@GetMapping("/defaultMenu")
	public List<MenuItem> getMenuItem() {
		List<Integer> folderIds = Arrays.asList(54, 53);
		List<MenuItem> defaultList = menuItemRepository.findByMenuFolderIdIn(folderIds);
		return defaultList;
	}*/

}