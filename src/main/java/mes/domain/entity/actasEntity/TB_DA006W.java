package mes.domain.entity.actasEntity;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "TB_DA006W") //WEB주문서HEAD정보 table
@Data
@NoArgsConstructor
public class TB_DA006W {
    @EmbeddedId
    private TB_DA006W_PK id;

    // 5 거래처코드
    @Column(name = "cltcd")
    private String cltcd;

    // 6 거래처명
    @Column(name = "cltnm")
    private String cltnm;

    // 7 사업자번호
    @Column(name = "saupnum")
    private String saupnum;

    // 8 업체우편번호
    @Column(name = "cltzipcd")
    private String cltzipcd;

    // 9 업체주소
    @Column(name = "cltaddr")
    private String cltaddr;

    // 9 상세주소
    @Column(name = "cltaddr02")
    private String cltaddr02;

    // 10 납품우편번호
    @Column(name = "delzipcd")
    private String delzipcd;

    // 11 납품주소
    @Column(name = "deladdr")
    private String deladdr;

    // 12 납기희망일
    @Column(name = "deldate")
    private String deldate;

    //출하일자
    @Column(name = "shipdate")
    private String shipdate;

    // 13 담당자
    @Column(name = "perid")
    private String perid;

    // 14 부서코드
    @Column(name = "divicd")
    private String divicd;

    // 15 내수구분
    @Column(name = "domcls")
    private String domcls;

    // 16 화폐단위
    @Column(name = "moncls")
    private String moncls;

    // 17 환율
    @Column(name = "monrate", precision = 18, scale = 4)
    private BigDecimal monrate;

    // 18 제목
    @Lob
    @Column(name = "remark")
    private String remark;

    // 19 발주담당
    @Column(name = "operid")
    private String operid;

    // 20 납품담당
    @Column(name = "dperid")
    private String dperid;

    // 21 확인자
    @Column(name = "sperid")
    private String sperid;

    // 22 상태구분
    @Column(name = "ordflag")
    private String ordflag;

    // 23 용도별
    @Column(name = "egrb")
    private String egrb;

    // 24 모델명
    @Column(name = "modeltxt")
    private String modeltxt;

    // 25 공급기준
    @Column(name = "setsamt")
    private Long setsamt;

    // 26 수량
    @Column(name = "setqty")
    private Long setqty;

    // 27 공급계
    @Column(name = "amount")
    private Long amount;

    // 28 외주계
    @Column(name = "outamt")
    private Long outamt;

    // 29 이윤
    @Column(name = "eyunamt")
    private Long eyunamt;

    // 30 개당이윤
    @Column(name = "pereyunamt")
    private Long pereyunamt;

    // 31 이윤율
    @Column(name = "eyunyul")
    private BigDecimal eyunyul;

    // 32 전체이윤
    @Column(name = "toteyunamt")
    private BigDecimal toteyunamt;

    // 33 프로젝트번호
    @Column(name = "projectno")
    private String projectno;

    // 34 입력일자
    @Column(name = "indate")
    private String indate;

    // 35 입력자
    @Column(name = "inperid", length = 10)
    private String inperid;

    // 36 연락처
    @Column(name = "telno", length = 20)
    private String telno;
    //모델 코드
    @Column(name = "pcode")
    private Long pcode;
    
}