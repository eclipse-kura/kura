/*******************************************************************************
 * Copyright (c) 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

var ForReading=1, ForWriting=2;

try {
	if( WScript.Arguments.length < 3 )
	{
		WScript.echo("Syntax: RIF.js filename string replacement");
		WScript.Quit(1);
	}

	var fnin = WScript.Arguments(0);	// arg1 = Filename
	var str  = WScript.Arguments(1);	// arg2 = String
	var repl = WScript.Arguments(2);	// arg3 = Replacement

	var fso = new ActiveXObject("Scripting.FileSystemObject");
	var fin  = fso.OpenTextFile(fnin, ForReading, false);
	var fout = fso.OpenTextFile(fnin+".new", ForWriting, true);
	var txt = fin.ReadAll();
	fin.Close();
	// Note, that the searched expressions contain paths and special chars, so we can't use RegExp
	// Thus we use normal string. In this case the function replaces only the 1st occurrence, so we do this in loop
	var origtxt, newtxt=txt;
	do {
		origtxt = newtxt;
		newtxt = newtxt.replace(str, repl);
	} while(origtxt != newtxt);
	fout.Write(newtxt);
	fout.Close();
	fso.DeleteFile(fnin);
	fso.MoveFile(fnin+".new", fnin);
}

catch (e) {
	WScript.echo("Error: " + e.message);
	WScript.Quit(1);
}
