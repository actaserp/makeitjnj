package mes.app.account;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import mes.app.MailService;
import mes.app.account.service.TB_RP940_Service;
import mes.app.account.service.TB_RP945_Service;
import mes.app.account.service.TB_XClientService;
import mes.app.account.service.UserProfileService;
import mes.app.system.service.AuthListService;
import mes.app.system.service.UserService;
import mes.domain.DTO.UserCodeDto;
import mes.domain.entity.*;
import mes.domain.entity.actasEntity.TB_XA012;
import mes.domain.entity.actasEntity.TB_XCLIENT;
import mes.domain.entity.actasEntity.TB_XCLIENTId;
import mes.domain.repository.*;
import mes.domain.repository.actasRepository.TB_XA012Repository;
import mes.domain.repository.actasRepository.TB_XClientRepository;
import org.hibernate.Hibernate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.util.StringUtils;

import org.springframework.web.servlet.ModelAndView;

import mes.domain.model.AjaxResult;
import mes.domain.security.CustomAuthenticationToken;
import mes.domain.security.Pbkdf2Sha256;
import mes.domain.services.AccountService;
import mes.domain.services.SqlRunner;

@Slf4j
@RestController
public class AccountController {

	@Autowired
	AccountService accountService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserCodeRepository userCodeRepository;

	@Autowired
	SqlRunner sqlRunner;

	@Autowired
	MailService emailService;

	@Autowired
	TB_XA012Repository tbXA012Repository;
	@Autowired
	TB_XClientService tbXClientService;
	@Autowired
	TB_XClientRepository tbXClientRepository;
	@Autowired
	JdbcTemplate jdbcTemplate;

	@Resource(name="authenticationManager")
	private AuthenticationManager authManager;
	@Autowired
	private UserGroupRepository userGroupRepository;

	private Boolean flag;
	private Boolean flag_pw;

	private final ConcurrentHashMap<String, String> tokenStore = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Long> tokenExpiry = new ConcurrentHashMap<>();
    @Autowired
    private UserService userService;

	@GetMapping("/login")
	public ModelAndView loginPage(
			HttpServletRequest request,
			HttpServletResponse response,
			HttpSession session, Authentication auth) {
		ModelAndView mv = new ModelAndView("login");

		Map<String, Object> userInfo = new HashMap<String, Object>();
		Map<String, Object> gui = new HashMap<String, Object>();

		mv.addObject("userinfo", userInfo);

		mv.addObject("gui", gui);
		if(auth!=null) {
			SecurityContextLogoutHandler handler =  new SecurityContextLogoutHandler();
			handler.logout(request, response, auth);
		}

		return mv;
	}

	@GetMapping("/logout")
	public void logout(
			HttpServletRequest request
			, HttpServletResponse response) throws IOException {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		SecurityContextLogoutHandler handler =  new SecurityContextLogoutHandler();

		this.accountService.saveLoginLog("logout", auth);

		handler.logout(request, response, auth);
		response.sendRedirect("/login");
	}

	@PostMapping("/login")
	public AjaxResult postLogin(
			@RequestParam("username") final String username,
			@RequestParam("password") final String password,
			final HttpServletRequest request) {
		// 여기로 들어오지 않음.

		AjaxResult result = new AjaxResult();

		HashMap<String, Object> data = new HashMap<String, Object>();
		result.data = data;

		UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(username, password);
		CustomAuthenticationToken auth = null;

		try{
			auth = (CustomAuthenticationToken)authManager.authenticate(authReq);


		} catch (InsufficientAuthenticationException e) {
			data.put("code", "null");
			return result;
		}catch (AuthenticationException e){
			//e.printStackTrace();
			data.put("code", "NOUSER");
			return result;
		}


		if(auth!=null) {
			User user = (User)auth.getPrincipal();

			if (!user.getActive()) {  // user.getActive()가 false인 경우
				data.put("code", "noactive");
			} else {
				data.put("code", "OK");

				try {
					this.accountService.saveLoginLog("login", auth);
				} catch (UnknownHostException e) {
					// Handle the exception (e.g., log it)
					e.printStackTrace();
				}
			}
		} else {
			result.success=false;
			data.put("code", "NOID");
		}

		SecurityContext sc = SecurityContextHolder.getContext();
		sc.setAuthentication(auth);

		HttpSession session = request.getSession(true);
		session.setAttribute("SPRING_SECURITY_CONTEXT", sc);

		return result;
	}

	@GetMapping("/account/myinfo")
	public AjaxResult getUserInfo(Authentication auth){
		User user = (User)auth.getPrincipal();
		AjaxResult result = new AjaxResult();

		Map<String, Object> dicData = userService.getUserInfo(user.getUsername());
		result.data = dicData;
		return result;
	}

	@PostMapping("/account/myinfo/password_change")
	public AjaxResult userPasswordChange(
			@RequestParam("name") final String name,
			@RequestParam("loginPwd") final String loginPwd,
			@RequestParam("loginPwd2") final String loginPwd2,
			Authentication auth
	) {

		User user = (User)auth.getPrincipal();
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

		user.setPassword(Pbkdf2Sha256.encode(loginPwd2));
		//user.getUserProfile().setName(name);
		this.userRepository.save(user);

		String sql = """
			UPDATE user_profile
			SET Name = :name,
				_modified = GETDATE(), 
				_modifier_id = :id
			WHERE id = :id
		""";

		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("name", name);
		dicParam.addValue("id", user.getId());
		this.sqlRunner.execute(sql, dicParam);
		return result;
	}


	/***
	 *  아이디 중복 확인
	 * **/
	@PostMapping("/useridchk")
	public AjaxResult IdChk(@RequestParam("userid") final String userid){

		AjaxResult result = new AjaxResult();


		Optional<User> user = userRepository.findByUsername(userid);


		if(!user.isPresent()){

			result.success = true;
			result.message = "사용할 수 있는 계정입니다.";
			return result;

		}else {
			result.success = false;
			result.message = "중복된 계정이 존재합니다.";
			return result;
		}


	}

	@PostMapping("/Register/save")
	@Transactional
	public AjaxResult RegisterUser(
			@RequestParam(value = "cltnm") String cltnm, // 업체명
			@RequestParam(value = "prenm") String prenm, // 대표자
			@RequestParam(value = "biztypenm") String biztypenm, // 업태
			@RequestParam(value = "bizitemnm") String bizitemnm, // 종목
			@RequestParam(value = "phone", required = false) String phone,
			@RequestParam(value = "tel", required = false) String tel,
			@RequestParam(value = "email", required = false) String email,
			@RequestParam(value = "id") String id,
			@RequestParam(value = "password") String password,
			@RequestParam(value = "postno") String postno,
			@RequestParam(value = "address1") String address1,
			@RequestParam(value = "address2") String address2
	) {
		AjaxResult result = new AjaxResult();
		MapSqlParameterSource Param = new MapSqlParameterSource();

		try {
			if (flag) {
				// 사용자 저장
				User user = User.builder()
						.username(id)
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

				userService.save(user); // User 저장

				jdbcTemplate.execute("SET IDENTITY_INSERT user_profile ON");
				// UserProfile 저장 (JDBC 사용)
				String sql = "INSERT INTO user_profile (_created, lang_code, Name, UserGroup_id, User_id) VALUES (?,?, ?, ?, ?)";
				jdbcTemplate.update(sql,
						new Timestamp(System.currentTimeMillis()), // 현재 시간
						"ko-KR", // lang_code (예: 한국어)
						prenm, // Name (사용자 이름)
						35 ,// UserGroup_id (일반거래처)
						user.getId() // User_id
				);
				jdbcTemplate.execute("SET IDENTITY_INSERT user_profile OFF");

				// TB_XA012에서 custcd와 spjangcd로 조회
				String custcd = "SWSPANEL";
				List<String> spjangcds = Arrays.asList("ZZ", "YY");

				List<TB_XA012> tbX_A012List = tbXA012Repository.findByCustcdAndSpjangcds(custcd, spjangcds);
				if (tbX_A012List.isEmpty()) {
					result.success = false;
					result.message = "custcd 및 spjangcd에 해당하는 데이터를 찾을 수 없습니다.";
					return result;
				}

				String fullAddress = address1 + (address2 != null && !address2.isEmpty() ? " " + address2 : "");

				// TB_XCLIENT 저장
				String maxCltcd = tbXClientRepository.findMaxCltcd(); // 최대 cltcd 조회
				String newCltcd = generateNewCltcd(maxCltcd); // 새로운 cltcd 생성

				TB_XCLIENT tbXClient = TB_XCLIENT.builder()
						.saupnum(id) // 사업자번호
						.prenm(prenm) // 대표자명
						.cltnm(cltnm)	//업체명
						.biztypenm(biztypenm) // 업태명
						.bizitemnm(bizitemnm) // 종목명
						.zipcd(postno) // 우편번호
						.cltadres(fullAddress) // 주소
						.telnum(tel) // 전화번호
						.hptelnum(phone) // 핸드폰번호
						.agneremail(email) // 담당자 email
						.id(new TB_XCLIENTId(custcd, newCltcd))

						// 기본값 설정된 필드들
						.rnumchk(String.valueOf(0))                 // rnumchk = 0
						.corpperclafi(String.valueOf(0))            // corpperclafi = 0 (법인구분, 기본 = 법인)
						.cltdv(String.valueOf(1))                   // cltdv = 1 (거래처구분)
						.prtcltnm(cltnm) 							   // prtcltnm = "인쇄 거래처명 - 거래처명"
						.foreyn(String.valueOf(0))                  // foreyn = 0
						.relyn(String.valueOf(0))                   // relyn = 0
						.bonddv(String.valueOf(0))                  // bonddv = 0
						/*.nation("KR")               // nation = "KR"*/
						.clttype(String.valueOf(2))                 // clttype = 2 (거래구분)
						.cltynm(String.valueOf(0))                  // cltynm = 0 (약명)
						.build();

				tbXClientService.save(tbXClient); // TB_XCLIENT 저장

				result.success = true;
				result.message = "등록이 완료되었습니다.";
				flag = false;
			} else {
				result.success = false;
				result.message = "코드가 인증되지 않았습니다.";
			}
		} catch (Exception e) {
			result.success = false;
			System.out.println("오류 발생: " + e.getMessage());
			System.out.println(e);
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
	
	@PostMapping("/account/updateUserInfo")
	@Transactional
	public AjaxResult updateUserInfo(@RequestBody Map<String, Object> userData) {
		System.out.println("받은 사용자 정보: " + userData);
		AjaxResult result = new AjaxResult();

		try {
			// 필수값 검증
			if (!userData.containsKey("login_id") || userData.get("login_id") == null) {
				result.success = false;
				result.message = "ID가 누락되었습니다.";
				return result;
			}

			// 사용자 정보 업데이트 처리
			String loginId = userData.get("login_id").toString();
			Optional<User> userOptional = userRepository.findByUsername(loginId);

			if (userOptional.isPresent()) {
				User user = userOptional.get();

				// 사용자 정보 업데이트
				if (user.getUserProfile() != null) {
					user.getUserProfile().setName(userData.getOrDefault("prenm", "").toString());
				}
				user.setEmail(userData.getOrDefault("email", "").toString());
				user.setPhone(userData.getOrDefault("phone", "").toString());
				user.setTel(userData.getOrDefault("tel", "").toString());

				// 비밀번호 업데이트 (새 비밀번호가 있을 경우만 처리)
				if (userData.containsKey("password")) {
					String password = userData.get("password").toString();
					if (!password.isEmpty()) {
						user.setPassword(Pbkdf2Sha256.encode(password)); // 비밀번호 암호화 후 저장
					}
				}
				// 저장
				userRepository.save(user);
			} else {
				result.success = false;
				result.message = "사용자를 찾을 수 없습니다.";
				return result;
			}

			// TB_XCLIENT 정보 업데이트
			Optional<TB_XCLIENT> clientOptional = tbXClientRepository.findBySaupnum(loginId);
			if (clientOptional.isPresent()) {
				TB_XCLIENT client = clientOptional.get();

				client.setCltnm(userData.getOrDefault("cltnm", "").toString());
				client.setBiztypenm(userData.getOrDefault("biztypenm", "").toString());
				client.setBizitemnm(userData.getOrDefault("bizitemnm", "").toString());
				client.setZipcd(userData.getOrDefault("postno", "").toString());

				// 주소 병합
				String address1 = userData.getOrDefault("address1", "").toString();
				String address2 = userData.getOrDefault("address2", "").toString();
				String fullAddress = address1 + (address2.isEmpty() ? "" : " | " + address2);
				client.setCltadres(fullAddress);
				// 저장
				tbXClientRepository.save(client);
			}

			if (userData.containsKey("User_id") && userData.containsKey("Name")) {
				String userId = userData.get("User_id").toString();
				String name = userData.get("Name").toString();

				String updateSql = """
                UPDATE user_profile
                SET 
                    Name = :name,
                    _modified = GETDATE()
                WHERE User_id = :userId;
            """;

				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("name", name);
				params.addValue("userId", userId);

				int rowsAffected = executeUpdate(updateSql, params);
				System.out.println("업데이트된 행 수: " + rowsAffected);
			}

			result.success = true;
			result.message = "사용자 정보가 성공적으로 업데이트되었습니다.";
		} catch (Exception e) {
			result.success = false;
			result.message = "정보 업데이트 중 오류가 발생했습니다.";
			e.printStackTrace();
		}

		return result;
	}


	public int executeUpdate(String sql, MapSqlParameterSource params) {
		try {
			return jdbcTemplate.update(sql, params);
		} catch (Exception e) {
			e.printStackTrace();
			return 0; // 실패 시 0 반환
		}
	}

	@PostMapping("/user-auth/searchAccount")
	public AjaxResult IdSearch(@RequestParam("usernm") final String usernm,
							   @RequestParam("userid1") final String userid1) {

		AjaxResult result = new AjaxResult();

		// 사업자 번호와 대표자를 기반으로 사용자 검색
		List<String> user = userRepository.findByFirstNameAndBusinessNumberNative(usernm, userid1);

		if (!user.isEmpty()) {
			result.success = true;
			result.data = user;
		} else {
			result.success = false;
			result.message = "해당 사용자가 존재하지 않습니다.";
		}
		return result;
	}

	@PostMapping("/user-auth/AuthenticationEmail")
	public AjaxResult PwSearch(@RequestParam("usernm") final String usernm,
							   @RequestParam("mail") final String mail){

		AjaxResult result = new AjaxResult();

		if(usernm.equals("empty")){
			sendEmailLogic(mail, "신규사용자");

			result.success = true;
			result.message = "인증 메일이 발송되었습니다.";
			return result;
		}

		int exists = userRepository.existsByUsernameAndEmail(usernm, mail);
		boolean flag = exists > 0;

		if(flag) {
			sendEmailLogic(mail, usernm);

			result.success = true;
			result.message = "인증 메일이 발송되었습니다.";
		}else {
			result.success = false;
			result.message = "해당 사용자가 존재하지 않습니다.";
		}

		return result;
	}



	private void sendEmailLogic(String mail, String prenm){
		Random random = new Random();
		int randomNum = 100000 + random.nextInt(900000); // 100000부터 999999까지의 랜덤 난수 생성
		String verificationCode = String.valueOf(randomNum); // 정수를 문자열로 변환
		emailService.sendVerificationEmail(mail, prenm, verificationCode);

		tokenStore.put(mail, verificationCode);
		tokenExpiry.put(mail, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3));

	}

	@PostMapping("/user-auth/SaveAuthenticationEmail")
	public AjaxResult saveMail(@RequestParam("usernm") final String usernm,
							   @RequestParam("prenm") final String prenm,
							   @RequestParam("mail") final String mail) {

		AjaxResult result = new AjaxResult();

		if (usernm.equals("empty")) {
			saveEmailLogic(mail, prenm);

			result.success = true;
			result.message = "인증 메일이 발송되었습니다.";
			return result;
		}

		int exists = userRepository.existsByUsernameAndEmail(usernm, mail);
		boolean flag = exists > 0;

		if (flag) {
			saveEmailLogic(mail, usernm);

			result.success = true;
			result.message = "인증 메일이 발송되었습니다.";
		} else {
			result.success = false;
			result.message = "해당 사용자가 존재하지 않습니다.";
		}

		return result;
	}


	private void saveEmailLogic(String mail, String usernm){
		Random random = new Random();
		int randomNum = 100000 + random.nextInt(900000); // 100000부터 999999까지의 랜덤 난수 생성
		String verificationCode = String.valueOf(randomNum); // 정수를 문자열로 변환
		emailService.saveVerificationEmail(mail, usernm, verificationCode);

		tokenStore.put(mail, verificationCode);
		tokenExpiry.put(mail, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3));

	}

	@PostMapping("/user-auth/verifyCode")
	public AjaxResult verifyCode(@RequestParam("code") final String code,
								 @RequestParam("mail") final String mail,
								 @RequestParam("password") final String password,
								 @RequestParam("userid") final String userid
	){

		AjaxResult result = new AjaxResult();
		result = verifyAuthenticationCode(code, mail);

		if(result.success){
			String pw = Pbkdf2Sha256.encode(password);


			userRepository.PasswordChange(pw, userid);


			result.success = true;
			result.message = "비밀번호가 변경되었습니다.";

			return result;
		}else{
			return result;
		}

		/*AjaxResult result = new AjaxResult();


		String storedToken = tokenStore.get(mail);

		if(storedToken != null && storedToken.equals(code)){
			long expiryTime = tokenExpiry.getOrDefault(mail, 0L);
			if(System.currentTimeMillis() > expiryTime){
				result.success = false;
				result.message = "인증 코드가 만료되었습니다.";
				tokenStore.remove(mail);
				tokenExpiry.remove(mail);
			} else {

				String pw = Pbkdf2Sha256.encode(password);


				userRepository.PasswordChange(pw, userid);


				result.success = true;
				result.message = "비밀번호가 변경되었습니다.";
			}
		}else {
			result.success = false;
			result.message = "인증 코드가 유효하지 않습니다.";
		}*/


	}

	private AjaxResult verifyAuthenticationCode(String code, String mail){

		AjaxResult result = new AjaxResult();

		String storedToken = tokenStore.get(mail);
		if(storedToken != null && storedToken.equals(code)){
			long expiryTime = tokenExpiry.getOrDefault(mail, 0L);
			if(System.currentTimeMillis() > expiryTime){
				result.success = false;
				result.message = "인증 코드가 만료되었습니다.";
				tokenStore.remove(mail);
				tokenExpiry.remove(mail);
			} else {
				result.success = true;
				result.message = "비밀번호가 변경되었습니다.";
			}
		}else{
			result.success = false;
			result.message = "인증 코드가 유효하지 않습니다.";
		}
		return result;
	}


	@GetMapping("/user-codes/parent")
	public List<UserCodeDto> getUserCodeByParentId(@RequestParam Integer parentId){

		List<UserCode> list = userCodeRepository.findByParentId(parentId);

		List<UserCodeDto> dtoList = list.stream()
				.map(userCode -> new UserCodeDto(
						userCode.getId(),
						userCode.getCode(),
						userCode.getValue()
				))
				.toList();

		return dtoList;
	}

	@GetMapping("/user-auth/type")
	public List<UserGroup> getUserAuthTypeAll(){
		return userGroupRepository.findAll();

	}

	@GetMapping("/user-codes/code")
	public List<UserCodeDto> getUserCodesByCode(@RequestParam Integer code) {

		List<UserCode> list = userCodeRepository.findByParentId(code);

		List<UserCodeDto> dtoList = list.stream()
				.map(userCode -> new UserCodeDto(
						userCode.getId(),
						userCode.getCode(),
						userCode.getValue()
				)).toList();

		return dtoList;
	}

	@PostMapping("/authentication")
	public AjaxResult Authentication(@RequestParam(value = "AuthenticationCode") String AuthenticationCode,
									 @RequestParam(value = "email", required = false) String email,
									 @RequestParam String type
	){

		AjaxResult result = verifyAuthenticationCode(AuthenticationCode, email);

		if(type.equals("new")){
			if(result.success){
				flag = true;
				result.message = "인증되었습니다.";

			}

		}else{
			if(result.success){
				flag_pw = true;
				result.message = "인증되었습니다.";
			}
		}

		return result;
	}

}