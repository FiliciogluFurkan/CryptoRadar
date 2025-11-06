package gtu.graduation.project.cryptoradar.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class Token {

    private final String name;
    private final String address;

    public static final Token ETH = new Token("ETH", null);
    public static final Token OTHER = new Token("OTHER", null);

    public final String name() { return this.name;}

    public final String address() {return this.address;}

}
