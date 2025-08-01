package mes.domain.entity.actasEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "model_history")
@Data
@NoArgsConstructor
public class ModelHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Column(name = "modelid")
  private String modelid;

  @Column(name = "custcd")
  private String custcd;

  @Column(name = "spjangcd")
  private String spjangcd;

  @Column(name = "reqdate")
  private String reqdate;

  @Column(name = "reqnum")
  private String reqnum;

  @Column(name = "cltcd")
  private String cltcd;

  @Column(name = "cltnm")
  private String cltnm;

  @Column(name = "modeltxt_current")
  private String modeltxt_current;

  @Column(name = "version_no")
  private int version_no;

  @Column(name = "prev_modeltxt")
  private String prev_modeltxt;

  @Column(name = "change_date")
  private String change_date;

  @Column(name = "changer_name")
  private String changer_name;
}
