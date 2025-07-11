package mes.domain.entity.actasEntity;

import lombok.*;

import javax.persistence.Embeddable;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@Data
public class TB_DA007W_PK implements Serializable {

    private String custcd;      // 회사코드
    private String spjangcd;    // 사업장코드
    private String reqdate;     // 주문일자
    private String reqnum;      // 주문번호
    private String reqseq;      // 주문순번
}
