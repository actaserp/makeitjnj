package mes.app.system;

import lombok.extern.slf4j.Slf4j;
import mes.app.UtilClass;
import mes.app.account.service.TB_XClientService;
import mes.app.system.service.UserService;
import mes.domain.entity.RelationData;
import mes.domain.entity.TB_RP945;
import mes.domain.entity.User;
import mes.domain.entity.actasEntity.TB_XA012;
import mes.domain.entity.actasEntity.TB_XCLIENT;
import mes.domain.entity.actasEntity.TB_XCLIENTId;
import mes.domain.model.AjaxResult;
import mes.domain.repository.RelationDataRepository;
import mes.domain.repository.TB_RP945Repository;
import mes.domain.repository.UserRepository;
import mes.domain.repository.actasRepository.TB_XA012Repository;
import mes.domain.repository.actasRepository.TB_XClientRepository;
import mes.domain.security.Pbkdf2Sha256;
import mes.domain.services.CommonUtil;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/system/user")
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RelationDataRepository relationDataRepository;

	@Autowired
	SqlRunner sqlRunner;

	@Autowired
	private TB_RP945Repository tB_RP945Repository;

	@Autowired
	TB_XClientService tbXClientService;

	@Autowired
	TB_XClientRepository tbXClientRepository;

	@Autowired
	TB_XA012Repository tbXA012Repository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@GetMapping("/read")
	public AjaxResult getUserList(@RequestParam(value = "cltnm", required = false) String cltnm, // 업체명
								  @RequestParam(value = "prenm", required = false) String prenm, // 대표자
								  @RequestParam(value = "biztypenm", required = false) String biztypenm, // 업태
								  @RequestParam(value = "bizitemnm", required = false) String bizitemnm, // 종목
								  @RequestParam(value = "email", required = false) String email,
								  @RequestParam(value = "spjangcd", required = false) String spjangcd,
								  Authentication auth){
		AjaxResult result = new AjaxResult();
		User user = (User)auth.getPrincipal();
		boolean superUser = user.getSuperUser();

		if (!superUser) {
			superUser = user.getUserProfile().getUserGroup().getCode().equals("dev");
		}

		List<Map<String, Object>> items = this.userService.getUserList(superUser, cltnm, prenm, biztypenm, bizitemnm, email, spjangcd);

		result.data = items;

		return result;
	}

	//사용자 관리 (더블 클릭) 사용
	@GetMapping("/detail")
	public AjaxResult getUserDetail(@RequestParam(value = "id", required = false) String id) {
		AjaxResult result = new AjaxResult();
		log.info("id: {}", id);

		try {
			if (id != null && !id.isEmpty()) {
				// ID로 특정 사용자 정보 조회
				Map<String, Object> userDetail = userService.getUserDetailById(id);

				// fullAddress 분리하여 address1과 address2로 추가
				if (userDetail.containsKey("fullAddress")) {
					String fullAddress = (String) userDetail.get("fullAddress");
					Map<String, String> addressParts = splitAddress(fullAddress);
					userDetail.put("address1", addressParts.get("address1"));
					userDetail.put("address2", addressParts.get("address2"));
				}

				result.success = true;
				result.data = userDetail;
				result.message = "데이터 조회 성공";
			} else {
				result.success = false;
				result.message = "유효한 ID가 제공되지 않았습니다.";
			}
		} catch (Exception e) {
			result.success = false;
			result.message = "데이터를 가져오는 중 오류가 발생했습니다.";
		}

		return result;
	}

	private Map<String, String> splitAddress(String fullAddress) {
		Map<String, String> addressParts = new HashMap<>();
		if (fullAddress != null && !fullAddress.isEmpty()) {
			String[] parts = fullAddress.split("\\|", 2); // "|"를 기준으로 분리
			addressParts.put("address1", parts[0].trim()); // 첫 번째 부분
			addressParts.put("address2", parts.length > 1 ? parts[1].trim() : ""); // 두 번째 부분
		} else {
			addressParts.put("address1", "");
			addressParts.put("address2", "");
		}
		return addressParts;
	}

	// 사용자 그룹 조회
	@GetMapping("/user_grp_list")
	public AjaxResult getUserGrpList(
			@RequestParam(value="id") Integer id,
			HttpServletRequest request) {

		List<Map<String, Object>> items = this.userService.getUserGrpList(id);
		AjaxResult result = new AjaxResult();
		result.data = items;
		return result;
	}

	@PostMapping("/save")
	@Transactional
	public AjaxResult saveUser(
			@RequestParam(value = "id", required = false) Integer id,
			@RequestParam(value = "cltnm") String cltnm,
			@RequestParam(value = "prenm") String prenm,
			@RequestParam(value = "biztypenm") String biztypenm,
			@RequestParam(value = "bizitemnm") String bizitemnm,
			@RequestParam(value = "tel") String tel,
			@RequestParam(value = "Phone") String phone,
			@RequestParam(value = "userid") String userid,
			@RequestParam(value = "email") String email,
			@RequestParam(value = "agencycd", required = false) String agencycd,
			@RequestParam(value = "authType", required = false) String authType,
			@RequestParam(value = "is_active") boolean isActive,
			@RequestParam(value = "postno") String postno,
			@RequestParam(value = "address1") String address1,
			@RequestParam(value = "address2") String address2,
			@RequestParam(value = "password") String password,
			@RequestParam(value = "UserGroup_id", required = false) Integer UserGroup_id,
			Authentication auth
	) {
		AjaxResult result = new AjaxResult();
		try {
			System.out.println("저장 로직 시작");

			boolean isNewUser = (id == null); // 새 사용자 여부 판단
			User user;

			if (isNewUser) {
				// 중복된 아이디 확인
				if (userRepository.findByUsername(userid).isPresent()) {
					result.success = false;
					result.message = "중복된 아이디가 존재합니다.";
					return result;
				}
				user = User.builder()
						.username(userid)
						.password(Pbkdf2Sha256.encode(password))
						.email(email)
						.first_name(prenm)
						.last_name("")
						.tel(tel)
						.active(true)
						.is_staff(false)
						.date_joined(new Timestamp(System.currentTimeMillis()))
						.superUser(false)
						.phone(phone)
						.spjangcd("ZZ")
						.build();
			} else {
				// 기존 사용자 업데이트
				user = userRepository.findById(id)
						.orElseThrow(() -> new RuntimeException("해당 사용자가 없습니다."));
				/*user.setPassword(Pbkdf2Sha256.encode(password)); */
				user.setEmail(email);
				user.setFirst_name(prenm);
				user.setLast_name("");
				user.setTel(tel);
				user.setActive(isActive);
				user.setPhone(phone);
				user.setAgencycd(agencycd);
			}

			// 사용자 저장
			user = userRepository.save(user);
			System.out.println(isNewUser ? "새 사용자 저장 완료: " : "기존 사용자 업데이트 완료: " + user.getUsername());

			// 사용자 프로필 처리
			String profileSql = """
			SET IDENTITY_INSERT user_profile ON;
		
			MERGE INTO user_profile AS target
			USING (SELECT ? AS _created, ? AS lang_code, ? AS Name, ? AS UserGroup_id, ? AS User_id) AS source
			ON target.User_id = source.User_id
			WHEN MATCHED THEN
				UPDATE SET Name = source.Name, UserGroup_id = source.UserGroup_id
			WHEN NOT MATCHED THEN
				INSERT (_created, lang_code, Name, UserGroup_id, User_id)
				VALUES (source._created, source.lang_code, source.Name, source.UserGroup_id, source.User_id);
		
			SET IDENTITY_INSERT user_profile OFF;
			""";

			jdbcTemplate.update(
					profileSql,
					new Timestamp(System.currentTimeMillis()), // _created
					"ko-KR",                                  // lang_code
					prenm,                                    // Name
					UserGroup_id,                             // UserGroup_id
					user.getId()                              // User_id
			);

			System.out.println("User Profile 저장 또는 업데이트 완료");



			// 거래처 처리 로직
			String custcd = "SWSPANEL";
			List<String> spjangcds = Arrays.asList("ZZ", "YY");

			List<TB_XA012> tbX_A012List = tbXA012Repository.findByCustcdAndSpjangcds(custcd, spjangcds);
			if (tbX_A012List.isEmpty()) {
				result.success = false;
				result.message = "custcd 및 spjangcd에 해당하는 데이터를 찾을 수 없습니다.";
				return result;
			}

			// 거래처 데이터 확인 및 저장
			String maxCltcd = tbXClientRepository.findMaxCltcd();
			String newCltcd = generateNewCltcd(maxCltcd);
			String fullAddress = address1 + (address2 != null && !address2.isEmpty() ? " " + address2 : "");

			Optional<TB_XCLIENT> existingClientOpt = tbXClientRepository.findBySaupnum(userid);
			TB_XCLIENT tbXClient;

			if (existingClientOpt.isPresent()) {
				tbXClient = existingClientOpt.get();
				tbXClient.setPrenm(prenm);
				tbXClient.setCltnm(cltnm);
				tbXClient.setBiztypenm(biztypenm);
				tbXClient.setBizitemnm(bizitemnm);
				tbXClient.setZipcd(postno);
				tbXClient.setCltadres(fullAddress);
				tbXClient.setTelnum(tel);
				tbXClient.setHptelnum(phone);
				tbXClient.setAgneremail(email);
			} else {
				tbXClient = TB_XCLIENT.builder()
						.saupnum(userid)
						.prenm(prenm)
						.cltnm(cltnm)
						.biztypenm(biztypenm)
						.bizitemnm(bizitemnm)
						.zipcd(postno)
						.cltadres(fullAddress)
						.telnum(tel)
						.hptelnum(phone)
						.agneremail(email)
						.id(new TB_XCLIENTId(custcd, newCltcd))
						.rnumchk("0")
						.corpperclafi("0")
						.cltdv("1")
						.prtcltnm(cltnm)
						.foreyn("0")
						.relyn("0")		//거래중지컬럼 0은 해제 상태
						.bonddv("0")
						.nation("KR")
						.clttype("2")
						.cltynm("0")
						.build();
			}

			tbXClientService.save(tbXClient);
			System.out.println("TB_XCLIENT 저장 완료");

			result.success = true;
			result.message = isNewUser ? "사용자가 성공적으로 등록되었습니다." : "사용자 정보가 성공적으로 업데이트되었습니다.";
		} catch (Exception e) {
			System.err.println("오류 발생: " + e.getMessage());
			e.printStackTrace();
			result.success = false;
			result.message = "사용자 정보를 저장하는 중 오류가 발생했습니다.";
		}
		return result;
	}

	// 새로운 cltcd 생성 메서드
	private String generateNewCltcd(String maxCltcd) {
		int newNumber = 1; // 기본값
		// 최대 cltcd 값이 null이 아니고 "SW"로 시작하는 경우
		if (maxCltcd != null && maxCltcd.startsWith("SW")) {
			String numberPart = maxCltcd.substring(2); // "SW"를 제외한 부분
			newNumber = Integer.parseInt(numberPart) + 1; // 숫자 증가
		}
		// 새로운 cltcd 생성: "SW" 접두사와 5자리 숫자로 포맷
		return String.format("SW%05d", newNumber);
	}
	@GetMapping("/check")
	public ResponseEntity<Map<String, Boolean>> checkUserExists(@RequestParam(value = "id") Integer id) {
		boolean exists = userRepository.findById(id).isPresent();
		Map<String, Boolean> response = new HashMap<>();
		response.put("exists", exists);
		return ResponseEntity.ok(response);
	}

	//사용자 관리 검색
	@GetMapping("/search")
	public ResponseEntity<Map<String, Object>> search(
			@RequestParam(name = "auth", required = false, defaultValue = "false") boolean auth,
			@RequestParam(required = false) String userGroup,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) String username) {

		// 검색 결과를 가져오는 로직
		List<Map<String, Object>> result = userService.searchData(userGroup, name, username);

		// 응답 데이터를 "data" 키로 래핑하여 JSON 형식으로 반환
		Map<String, Object> response = new HashMap<>();
		response.put("data", result);
		return ResponseEntity.ok(response);
	}

	// user 삭제
	@Transactional
	@PostMapping("/delete")
	public AjaxResult deleteUser(@RequestParam("id") String id,
								 @RequestParam(value = "username", required = false) String username,
								 @RequestParam boolean auth) {
		AjaxResult result = new AjaxResult();
		System.out.println("삭제 들어옴");

		if(auth){
			result.success = false;
			result.message = "슈퍼유저는 수정 및 삭제가 불가능합니다.";
			return result;
		}
		System.out.println("조회하려는 username: " + UtilClass.removeBrackers(username));
		Optional<User> user = userRepository.findByUsername(UtilClass.removeBrackers(username));
		if (user.isPresent()) {
			System.out.println("User 조회 성공: " + user.get());
			tbXClientRepository.deleteBySaupnum(user.get().getUsername());
		} else {
			System.out.println("User 조회 실패. username이 존재하지 않습니다.");
		}

		Integer userid  = Integer.parseInt(UtilClass.removeBrackers(id));
		this.userRepository.deleteById(userid);

		return result;
	}

	@GetMapping("/checkUserId")
	public ResponseEntity<Map<String, Boolean>> checkUserId(@RequestParam String userid) {
		Map<String, Boolean> response = new HashMap<>();
		boolean exists = userService.isUserIdExists(userid); // username 중복 체크
		response.put("exists", exists);
		return ResponseEntity.ok(response);
	}


	@Transactional
	@PostMapping("/activate")
	public AjaxResult Activate(@RequestParam Integer id,
							   @RequestParam boolean is_active
	){

		AjaxResult result = new AjaxResult();
		Optional<User> user = userRepository.findById(id);
		if(user.isPresent())
		{
			User u = user.get();
			if(u.getSuperUser()){
				result.success = false;
				result.message = "슈퍼유저는 변경이 불가능합니다.";

			}else{
				//save호출
				u.setActive(is_active);
				result.success = true;
				result.message = "변경되었습니다.";
			}
		}else{
			result.success = false;
			result.message = "해당 사용자가 존재하지 않습니다.";

		}
		return result;
	}

	@PostMapping("/modfind")
	public AjaxResult getBtId(@RequestBody String userid){

		///userid = new UtilClass().removeBrackers(userid);
		AjaxResult result = new AjaxResult();

		List<TB_RP945> tbRp945 =  tB_RP945Repository.findByUserid(userid);

		if(!tbRp945.isEmpty()){
			result.success = true;
			result.data = tbRp945;
		}else{
			result.success = false;
			result.message = "해당 유저에 대한 권한상세정보가 없습니다.";
		}

		return result;
	}


	// user 패스워드 셋팅
	@PostMapping("/passSetting")
	@Transactional
	public AjaxResult userPassSetting(
			@RequestParam(value="id", required = false) Integer id,
			@RequestParam(value="pass1", required = false) String loginPwd,
			@RequestParam(value="pass2", required = false) String loginPwd2,
			Authentication auth
	) {

		User user = null;
		AjaxResult result = new AjaxResult();

		if (StringUtils.hasText(loginPwd)==false | StringUtils.hasText(loginPwd2)==false) {
			result.success=false;
			result.message="The verification password is incorrect.";
			return result;
		}

		if(loginPwd.equals(loginPwd2)==false) {
			result.success=false;
			result.message="The verification password is incorrect.";
			return result;
		}

		user = this.userRepository.getUserById(id);
		user.setPassword(Pbkdf2Sha256.encode(loginPwd));
		this.userRepository.save(user);

		return result;
	}

	@PostMapping("/save_user_grp")
	@Transactional
	public AjaxResult saveUserGrp(
			@RequestParam(value="id") Integer id,
			@RequestBody MultiValueMap<String,Object> Q,
			Authentication auth
	) {

		User user = (User)auth.getPrincipal();;

		AjaxResult result = new AjaxResult();

		List<Map<String, Object>> items = CommonUtil.loadJsonListMap(Q.getFirst("Q").toString());

		List<RelationData> rdList = this.relationDataRepository.findByDataPk1AndTableName1AndTableName2(id,"auth_user", "user_group");

		// 등록된 그룹 삭제
		for (int i = 0; i < rdList.size(); i++) {
			this.relationDataRepository.deleteById(rdList.get(i).getId());
		}

		this.relationDataRepository.flush();
		for (int i = 0; i< items.size(); i++) {

			String check = "";

			if (items.get(i).get("grp_check") != null) {
				check = items.get(i).get("grp_check").toString();
			}

			if (check.equals("Y")) {
				RelationData rd = new RelationData();
				rd.setDataPk1(id);
				rd.setTableName1("auth_user");
				rd.setDataPk2(Integer.parseInt(items.get(i).get("grp_id").toString()));
				rd.setTableName2("user_group");
				rd.setRelationName("auth_user-user_group");
				rd.setChar1("Y");
				rd.set_audit(user);

				this.relationDataRepository.save(rd);
			}
		}

		return result;
	}
}
