/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.swt.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import com.minres.scviewer.database.ISignal;
import com.minres.scviewer.database.ISignalChange;
import com.minres.scviewer.database.ISignalChangeMulti;
import com.minres.scviewer.database.ISignalChangeSingle;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformColors;

public class SignalPainter extends TrackPainter  {

	/**
	 * 
	 */
	private final WaveformCanvas waveCanvas;
	private ISignal<? extends ISignalChange> signal;

	public SignalPainter(WaveformCanvas txDisplay, boolean even, TrackEntry trackEntry) {
		super(trackEntry, even);
		this.waveCanvas = txDisplay;
		this.signal=trackEntry.getSignal();
	}

	public void paintArea(GC gc, Rectangle area) {	
		if(trackEntry.selected)
			gc.setBackground(this.waveCanvas.colors[WaveformColors.TRACK_BG_HIGHLITE.ordinal()]);
		else
			gc.setBackground(this.waveCanvas.colors[even?WaveformColors.TRACK_BG_EVEN.ordinal():WaveformColors.TRACK_BG_ODD.ordinal()]);
		gc.setFillRule(SWT.FILL_EVEN_ODD);
		gc.fillRectangle(area);
		Entry<Long, ? extends ISignalChange> firstChange=signal.getEvents().floorEntry(area.x*this.waveCanvas.getScaleFactor());
		Entry<Long, ? extends ISignalChange> lastTx=signal.getEvents().ceilingEntry((area.x+area.width)*this.waveCanvas.getScaleFactor());
		if(firstChange==null){
			if(lastTx==null) return;
			firstChange = signal.getEvents().firstEntry();
		} else if(lastTx==null){
			lastTx=signal.getEvents().lastEntry();
		}
		gc.setForeground(this.waveCanvas.colors[WaveformColors.LINE.ordinal()]);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		Entry<Long, ? extends ISignalChange> left=firstChange;
		NavigableMap<Long,? extends ISignalChange> entries=signal.getEvents().subMap(firstChange.getKey(), false, lastTx.getKey(), true);
		Entry<Long, ? extends ISignalChange> right=entries.firstEntry();
		if(right==null) {
			right=signal.getEvents().higherEntry(left.getKey());			
		}
		int yOffsetT = this.waveCanvas.getTrackHeight()/5+area.y;
		int yOffsetM = this.waveCanvas.getTrackHeight()/2+area.y;
		int yOffsetB = 4*this.waveCanvas.getTrackHeight()/5+area.y;
		int xBegin= (int)(left.getKey()/this.waveCanvas.getScaleFactor());
		long lxEnd = right.getKey()/this.waveCanvas.getScaleFactor();
		int xEnd= lxEnd>Integer.MAX_VALUE?Integer.MAX_VALUE:(int)lxEnd;
		if(left.getValue() instanceof ISignalChangeSingle){
			boolean jump=false;
			if(xEnd==xBegin) {
				xEnd=xBegin+1;
				long eTime=xEnd*this.waveCanvas.getScaleFactor();
				right=entries.higherEntry(eTime);
				jump=true;
			}
			do {
				int yOffset = yOffsetM;
				Color color = this.waveCanvas.colors[WaveformColors.SIGNALX.ordinal()];
				if(xEnd>(xBegin+1)){
					switch(((ISignalChangeSingle) left.getValue()).getValue()){
					case '1':
						color=this.waveCanvas.colors[WaveformColors.SIGNAL1.ordinal()];
						yOffset = yOffsetT;
						break;
					case '0':
						color=this.waveCanvas.colors[WaveformColors.SIGNAL0.ordinal()];
						yOffset = yOffsetB;
						break;
					case 'Z':
						color=this.waveCanvas.colors[WaveformColors.SIGNALZ.ordinal()];
						break;
					default:	
					}
					gc.setForeground(color);
					gc.drawLine(xBegin, yOffset, xEnd, yOffset);
					int yNext =  yOffsetM;
					switch(((ISignalChangeSingle) right.getValue()).getValue()){
					case '1':
						yNext = yOffsetT;
						break;
					case '0':
						yNext = yOffsetB;
						break;
					default:	
					}
					gc.drawLine(xEnd, yOffset, xEnd, yNext);
				} else {
					gc.setForeground(color);
					gc.drawLine(xBegin, yOffsetT, xBegin, yOffsetT);
				}
				left=right;
				xBegin=xEnd;
				right=entries.higherEntry(left.getKey());
				if(right!=null) {
					xEnd= (int)(right.getKey()/this.waveCanvas.getScaleFactor());
					jump=false;
					if(xEnd==xBegin) {
						xEnd=xBegin+1;
						long eTime=xEnd*this.waveCanvas.getScaleFactor();
						right=entries.higherEntry(eTime);
						jump=true;
					}
				}
			}while(right!=null);
		} else if(left.getValue() instanceof ISignalChangeMulti){
			if(xEnd==xBegin) {
				xEnd=xBegin+1;
				long eTime=xEnd*this.waveCanvas.getScaleFactor();
				right=entries.higherEntry(eTime);
			}
			do{
				Color colorBorder = this.waveCanvas.colors[WaveformColors.SIGNAL0.ordinal()];
				ISignalChangeMulti last = (ISignalChangeMulti) left.getValue();
				if(last.getValue().toString().contains("X")){
					colorBorder=this.waveCanvas.colors[WaveformColors.SIGNALX.ordinal()];
				}else if(last.getValue().toString().contains("Z")){
					colorBorder=this.waveCanvas.colors[WaveformColors.SIGNALZ.ordinal()];
				}
				int width=xEnd-xBegin;
				if(width>1) {
					int[] points = {
							xBegin,yOffsetM, 
							xBegin+1,yOffsetT, 
							xEnd-1,yOffsetT, 
							xEnd,yOffsetM, 
							xEnd-1,yOffsetB, 
							xBegin+1,yOffsetB
					};
					gc.setForeground(colorBorder);
					gc.drawPolygon(points);
					gc.setForeground(this.waveCanvas.colors[WaveformColors.SIGNAL_TEXT.ordinal()]);
					int size = gc.getDevice().getDPI().y * gc.getFont().getFontData()[0].getHeight()/72;
					if(xBegin<area.x) xBegin=area.x;
					if(width>6) {
						Rectangle old = gc.getClipping();
						gc.setClipping(xBegin+3, yOffsetT, xEnd-xBegin-5, yOffsetB-yOffsetT);
						gc.drawText("h'"+last.getValue().toHexString(), xBegin+3, yOffsetM-size/2-1);
						gc.setClipping(old);
					}
				} else {
					gc.setForeground(colorBorder);
					gc.drawLine(xEnd, yOffsetT, xEnd, yOffsetB);
				}
				left=right;
				xBegin=xEnd;
				right=entries.higherEntry(left.getKey());
				if(right!=null) {
					xEnd= (int)(right.getKey()/this.waveCanvas.getScaleFactor());
					if(xEnd==xBegin) {
						xEnd=xBegin+1;
						long eTime=xEnd*this.waveCanvas.getScaleFactor();
						right=entries.higherEntry(eTime);
					}
				}
			}while(right!=null);
		}
	}


	public ISignal<? extends ISignalChange> getSignal() {
		return signal;
	}

}