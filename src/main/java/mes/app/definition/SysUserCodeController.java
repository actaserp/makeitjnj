package mes.app.definition;

import mes.app.definition.service.SysUserCodeService;
import mes.domain.entity.SystemCode;
import mes.domain.entity.User;
import mes.domain.entity.UserCode;
import mes.domain.model.AjaxResult;
import mes.domain.repository.SysCodeRepository;
import mes.domain.repository.UserCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/definition/code")
public class SysUserCodeController {
	
	
	@Autowired
	private SysUserCodeService syscodeService;

	@Autowired
	UserCodeRepository userCodeRepository;


	@Autowired
	SysCodeRepository sysCodeRepository;

	
	@GetMapping("/read")
	public AjaxResult getCodeList(
			@RequestParam("txtCode") String txtCode
			) {
		
		List<Map<String, Object>> items = this.syscodeService.getCodeList(txtCode);
		AjaxResult result = new AjaxResult();
		
		result.data = items;
		return result;
	}

	@GetMapping("/SystemCoderead")
	public AjaxResult getSystemCodeList(
			@RequestParam("txtCode") String txtCode,
			@RequestParam("txtCodeType") String txtCodeType,
			@RequestParam(value ="txtDescription") String txtDescription
	) {

		List<Map<String, Object>> items = this.syscodeService.getSystemCodeList(txtCode,txtCodeType,txtDescription);
		AjaxResult result = new AjaxResult();

		result.data = items;
		return result;
	}
	
	@GetMapping("/detail")
	public AjaxResult getCode(@RequestParam("id") int id) {
		Map<String, Object> item = this.syscodeService.getCode(id);
		
		AjaxResult result = new AjaxResult();
		result.data = item;
		return result;
	}

	@GetMapping("/Systemcodedetail")
	public AjaxResult getSystemCode(@RequestParam("id") int id) {
		Map<String, Object> item = this.syscodeService.getSystemcCode(id);

		AjaxResult result = new AjaxResult();
		result.data = item;
		return result;
	}

	
	@PostMapping("/save")
	public AjaxResult saveCode(
			@RequestParam(value="id", required=false) Integer id,
			@RequestParam("name") String value,
			@RequestParam("code") String code,
			@RequestParam(value="parent_id" , required=false) Integer parent_id,
			@RequestParam("description") String description,
			HttpServletRequest request,
			Authentication auth) {
		User user = (User)auth.getPrincipal();
		
		UserCode c = null;
		
		if(id == null) {
			c = new UserCode();
		} else {
			c = this.userCodeRepository.getUserCodeById(id);
		}
		c.setValue(value);
		c.setCode(code);
		c.setDescription(description);
		c.setParentId(parent_id);
		c.set_audit(user);
		
		c = this.userCodeRepository.save(c);
		
		AjaxResult result = new AjaxResult();
		result.data = c;
		
		return result;
	}

	@PostMapping("/Systemcodesave")
	public AjaxResult Systemcodesave(
			@RequestParam(value="id", required=false) Integer id,
			@RequestParam("code_type") String code_type,
			@RequestParam("name") String value,
			@RequestParam("code") String code,
			@RequestParam("description") String description,
			@RequestParam(value ="spjangcd") String spjangcd,
			HttpServletRequest request,
			Authentication auth) {
		User user = (User)auth.getPrincipal();

		SystemCode s = null;

		if(id == null) {
			s = new SystemCode();
		} else {
			s = this.sysCodeRepository.getSysCodeById(id);
		}

		s.setCodeType(code_type);
		s.setValue(value);
		s.setCode(code);
		s.setDescription(description);
		s.set_audit(user);

		s = this.sysCodeRepository.save(s);

		AjaxResult result = new AjaxResult();
		result.data = s;

		return result;
	}


	@PostMapping("/delete")
	public AjaxResult deleteCode(@RequestParam("id") Integer id) {
		this.userCodeRepository.deleteById(id);
		AjaxResult result = new AjaxResult();
		
		return result;
	}


	@PostMapping("/SystemCodedelete")
	public AjaxResult deleteSystemCode(@RequestParam("id") Integer id) {
		this.sysCodeRepository.deleteById(id);
		AjaxResult result = new AjaxResult();

		return result;
	}

	@GetMapping("/getvalue")
	public AjaxResult getValue(@RequestParam("code") String code) {
		Map<String, Object> item = this.syscodeService.getValue(code);

		AjaxResult result = new AjaxResult();
		result.data = item;
		return result;
	}

	
}