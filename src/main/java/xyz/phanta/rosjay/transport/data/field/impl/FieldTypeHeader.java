package xyz.phanta.rosjay.transport.data.field.impl;

import xyz.phanta.rosjay.rospkg.std_msgs.Header;

public class FieldTypeHeader extends FieldTypeMessage<Header> {

    public static final FieldTypeHeader TYPE = new FieldTypeHeader();

    private FieldTypeHeader() {
        super(Header.TYPE);
    }

    @Override
    public Header getDefaultValue() {
        return Header.TYPE.newInstance();
    }

    @Override
    public String toString() {
        return "Header";
    }

}
