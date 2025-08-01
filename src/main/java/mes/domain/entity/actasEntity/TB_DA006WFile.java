package mes.domain.entity.actasEntity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "tb_DA006WFILE") //주문등록 head file 정보
@NoArgsConstructor
@Data
public class TB_DA006WFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer fileid;

    @Column(name = "filepath")  // 파일경로
    String filepath;

    @Column(name = "custcd")  //
    String custcd;

    @Column(name = "spjangcd")  //
    String spjangcd;

    @Column(name = "reqdate")  //
    String reqdate;

    @Column(name = "reqnum")  //
    String reqnum;

    @Column(name = "inuserid")  //
    String inuserid;

    @Column(name = "inusernm")  //
    String inusernm;

    @Column(name = "filesvnm")  // 파일uuid nvarchar
    String filesvnm;

    @Column(name = "fileornm")  // 파일원본이름 nvarchar
    String fileornm;

    @Column(name = "filesize")  // 파일용량 nvarchar
    BigDecimal filesize;

    @Column(name = "filerem")  // 파일내용 nvarchar
    String filerem;

    @Column(name = "fileextns")  // 파일내용 nvarchar
    String fileextns;

    @Column(name = "fileurl")  // 파일내용
    private String fileurl;

    @Column(name = "indatem")
    private Timestamp indatem;

    @Column(name = "workdt")
    private Timestamp workdt;

}
