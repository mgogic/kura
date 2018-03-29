/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.rest.device;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

/**
 * WriteRequest class represents payload sent in POST request
 * in order to write data to device channels .
 * Class WriteRequest implements {@code Validable} interface
 */
public class WriteRequest implements Validable {

    /**
     * field name represents name of specific channel on which we want to perform write command
     */
    private String name;
    /**
     * Field type contains all the required data type constants required for representing Java data types as TypedValue
     * This field represent type of data that user wants to write on component
     * 
     * @see org.eclipse.kura.type.TypedValue
     * @see org.eclipse.kura.type.DataType
     */
    private DataType type;
    /**
     * Field value represents value sent in payload to write on component
     */
    private String value;

    /**
     * Method returns component name from WriteRequest
     * 
     * @return component name from WriteRequest
     */
    public String getName() {
        return name;
    }

    /**
     * Method returns type of data sent in payload
     * 
     * @return field type from WriteRequest
     */
    public DataType getType() {
        return type;
    }

    /**
     * Method returns type of data as TypedValue
     * 
     * @see org.eclipse.kura.type.TypedValue
     * 
     * @return field value from WriteRequest translated to TypedValue
     */
    public TypedValue<?> getValue() {
        return TypedValues.parseTypedValue(type, value);
    }

    /**
     * Method returns ChannelRecord object
     * This method creates write record corresponding to
     * ChannelRecord object, using name and value from WriteRequest
     * 
     * @see org.eclipse.kura.channel.ChannelRecord
     * 
     * @return prepared ChannelRecord, ready for writing to component
     */
    public ChannelRecord toChannelRecord() {
        return ChannelRecord.createWriteRecord(name, getValue());
    }

    /*
     * Overrode toString() method, for representing
     * WriteRequest object as String
     * 
     * @see java.lang.Object#toString()
     * 
     * @return String representation of WriteRequest object
     */
    @Override
    public String toString() {
        return "WriteRequest [name=" + name + ", type=" + type + ", value=" + getValue() + "]";
    }

    /*
     * Overrode isValid() method from Validable interface
     * Method checks is WriteRequest sent in payload valid
     * 
     * @return method returns true only if name, type and value are not null
     * 
     * @see org.eclipse.kura.internal.rest.device.Validable#isValid()
     */
    @Override
    public boolean isValid() {
        return name != null && type != null && value != null;
    }

}
