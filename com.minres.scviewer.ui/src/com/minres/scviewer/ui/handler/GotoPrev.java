package com.minres.scviewer.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.minres.scviewer.ui.TxEditorPart;

public class GotoPrev extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if(editor instanceof TxEditorPart){
			((TxEditorPart)editor).moveSelection(GotoDirection.PREV);
		}
		return null;
	}

}