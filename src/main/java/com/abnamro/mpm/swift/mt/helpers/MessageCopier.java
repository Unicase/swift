package com.abnamro.mpm.swift.mt.helpers;

import com.prowidesoftware.swift.model.*;

/**
 * Service for creating deep copies of SWIFT messages.
 */
public class MessageCopier {

    public SwiftMessage copyMessage(SwiftMessage source) {
        SwiftMessage target = new SwiftMessage();

        // Copy Block 1
        if (source.getBlock1() != null) {
            target.setBlock1(new SwiftBlock1(source.getBlock1().getValue()));
        }

        // Copy Block 2
        if (source.getBlock2() != null) {
            SwiftBlock2Output block2 = new SwiftBlock2Output();
            block2.setSender(((SwiftBlock2Input) source.getBlock2()).getReceiverBIC());
            target.setBlock2(block2);
        }

        // Copy Block 3
        if (source.getBlock3() != null) {
            SwiftBlock3 block3 = new SwiftBlock3();
            source.getBlock3().getTags().stream().map(tag -> new Tag(tag.getName(), tag.getValue())).forEach(block3::append);
            target.setBlock3(block3);
        }

        // Copy Block 4
        if (source.getBlock4() != null) {
            SwiftBlock4 block4 = new SwiftBlock4();
            source.getBlock4().getTags().stream().map(tag -> new Tag(tag.getName(), tag.getValue())).forEach(block4::append);
            target.setBlock4(block4);
        }

        return target;
    }
}

