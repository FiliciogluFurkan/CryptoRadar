package gtu.graduation.project.cryptoradar.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Token {

    private final String name;
    private final String address;

    public static final Token NATIVE = new Token("ETH", null);

    public final String name() { return this.name;}

    public final String address() {return this.address;}

}
