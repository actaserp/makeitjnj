package mes.domain.entity.actasEntity;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;


@Entity
@Data
@Table(name = "TB_DA007W") //주문서BODY 정보
@NoArgsConstructor
public class TB_DA007W {

    @EmbeddedId
    private TB_DA007W_PK id;

    // 6 모품목코드
    @Column(name = "pcode")
    private Long pcode;

    // 7 모델명
    @Column(name = "modelnm")
    private String modelnm;

    // 8 자재품목코드
    @Column(name = "japcode")
    private String japcode;

    // 9 부품명
    @Column(name = "pname")
    private String pname;

    // 10 비고
    @Column(name = "remark")
    private String remark;

    // 11 작업방식
    @Column(name = "jobflag")
    private String jobflag;

    // 12 set단가
    @Column(name = "setamt")
    private BigDecimal setamt;

    // 13 판매단가
    @Column(name = "saleamt")
    private BigDecimal saleamt;

    // 14 수량
    @Column(name = "QTY")
    private Double qty;

    // 15 제품가
    @Column(name = "uamt")
    private Double uamt;

    // 16 단가이력
    @Column(name = "uamttxt")
    private String uamttxt;

    // 22 입력일자
    @Column(name = "indate")
    private String indate;

    // 23 입력자
    @Column(name = "inperid")
    private String inperid;

    // 24 옵션 및 요청사항
    @Lob
    @Column(name = "ordtext")
    private String ordtext;

    @Column(name = "stframedv")
    private String stframedv;

    @Column(name = "stexplydv")
    private String stexplydv;

}