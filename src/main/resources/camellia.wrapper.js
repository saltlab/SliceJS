var counter = 0;

function _dynoRead(varName, value, lineNo, fileName, id) {
    // Send info here
    send(JSON.stringify({
             messageType: "VARIABLE_READ",
             lineNo: lineNo,
             value: Object.prototype.toString.call(value),
             variable: varName,
             order: counter++,
             file: fileName,
             definingFunction: id
    }));
    
    return value;
}

function _dynoReadAsArg(varName, value, functionName, argNumber, lineNo, fileName, id) {
    send(JSON.stringify({
             messageType: "READ_AS_ARGUMENT",
             lineNo: lineNo,
             value: Object.prototype.toString.call(value),
             variable: varName,
             argumentNumber: argNumber,
             functionName: functionName,
             file: fileName,
             definingFunction: id,
             order: counter++,
    }));
    return value;
}

function _dynoWrite(varName, newValue, readFrom, lineNo, fileName, id) {
window.console.log("_dynoWrite",  varName, readFrom);
    // Send info here
    send(JSON.stringify({
             messageType: "VARIABLE_WRITE",
             lineNo: lineNo,
             value: Object.prototype.toString.call(newValue),
             variable: varName,
             alias: readFrom,
             order: counter++,
             file: fileName,
             globalID: id
    }));
    return newValue;
}

function _dynoWriteAug(varName, newValue, readFrom, lineNo, fileName, id) {
    // Augmented assginment
    // Send info here
    send(JSON.stringify({
             messageType: "VARIABLE_WRITE_ADDSUB",
             lineNo: lineNo,
             value: Object.prototype.toString.call(newValue),
             variable: varName,
             alias: readFrom,
             order: counter++,
             file: fileName,
             globalID: id
    }));
    return newValue;
}

function _dynoWriteReturnValue(varName, returnValue, method, lineNo, fileName, id) {
    // Send info here
    send(JSON.stringify({
             messageType: "WRITE_RETURN_VALUE",
             lineNo: lineNo,
             value: Object.prototype.toString.call(returnValue),
             file: fileName,
             variable: varName,
             order: counter++,
             functionName: method,
             globalID: id
    }));
    
    return returnValue;
}

function _dynoReadProp(baseObject, propAsString, lineNo, fileName, id) {
window.console.log("_dynoReadProp",  baseObject, propAsString);
    // Send info here
    send(JSON.stringify({
             messageType: "PROPERTY_READ",
             lineNo: lineNo,
             variable: baseObject,
             property: propAsString,
             file: fileName,
             order: counter++,
             functionFlag: false,
             globalID: id
    }));

    return propAsString;
}

function _dynoFunc(baseObject, propAsString, lineNo, fileName, id) {
    // Send info here
    send(JSON.stringify({
             messageType: "PROPERTY_READ",
             lineNo: lineNo,
             variable: baseObject,
             file: fileName,
             property: propAsString,
             order: counter++,
             functionFlag: true,
             globalID: id
    }));

    return propAsString;
}

function _dynoWriteArg(varName, newValue, functionName, argNum, lineNo, fileName, id) {
    // Send info here
    send(JSON.stringify({
             messageType: "WRITE_AS_ARGUMENT",
             lineNo: lineNo,
             value: Object.prototype.toString.call(newValue),
             file: fileName,
             variable: varName,
             functionName: functionName,
             argumentNumber: argNum,
             order: counter++,
             globalID: id
    }));
    return newValue;
}

function _dynoReturnValue(functionName, returnValue, fileName, lineNo, id) {
    // Send info here
    send(JSON.stringify({
             messageType: "RETURN_VALUE",
             lineNo: lineNo,
             value: Object.prototype.toString.call(returnValue),
             file: fileName,
             functionName: functionName,
             variable: functionName,
             order: counter++,
             globalID: id
    }));
    return returnValue;
}
