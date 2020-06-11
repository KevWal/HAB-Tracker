package com.brejza.matt.habmodem;

import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import java.util.Arrays;
//import android.util.Log;

public class Waterfall {

	private int[] _grad;
	private int _grad_max;
	//KEW Scale fftin by this number set in Advanced Settings
	private double _volume = 1;
	
	private Bitmap im1;
	private Bitmap im2;
	private boolean _active_image = true;
	private int line1 = 399;
	private int line2 = 199;
	private int _imageHeight = 200;
	
    public Waterfall(Bitmap gradient, int imageHeight)
    {
		_imageHeight = imageHeight;
		line1 = 2*imageHeight - 1;
		line2 = line1 - imageHeight;
		im1 = Bitmap.createBitmap(512, 2*imageHeight, Bitmap.Config.ARGB_8888);
		im2 = Bitmap.createBitmap(512, 2*imageHeight, Bitmap.Config.ARGB_8888);
		
		_grad = new int[gradient.getWidth()];
		for (int i = 0; i < gradient.getWidth(); i++)
		{
			_grad[i] = gradient.getPixel(i,0);
		}
		_grad_max = gradient.getWidth();
	    System.out.println("KEW Setup Waterfall with: " + _grad_max + " colours from grad.png.");
		System.out.println("KEW _grad array: " + Arrays.toString(_grad));
    }

	
	public Bitmap updateLine(double[] fftin, int f1, int f2)
	{
		if (fftin.length != 512)
			return null;
		
		if (_imageHeight <= 0)
			return null;

		int gradPowerIndex;
		//double _fftinMin = Double.MAX_VALUE;
		//double _fftinAvg = 0;
		//double _fftinSum = 0;
		//double _fftinMax = Double.MIN_VALUE;
		
		for (int i = 0; i < 512; i++)
		{
			//KEW Original gradPowerIndex = -40 + (10 * (int)(Math.log10(fftin[i])));
			gradPowerIndex = -40 + (10 * (int)(Math.log10(Math.pow(fftin[i], _volume))));

			//KEW Calculate Min and Max
			//if(fftin[i] < _fftinMin) _fftinMin = fftin[i];
			//if(fftin[i] > _fftinMax) _fftinMax = fftin[i];
			//_fftinSum = _fftinSum + fftin[i];

			if (gradPowerIndex < 0)
				gradPowerIndex = 0;
			else if (gradPowerIndex >= _grad_max)
				gradPowerIndex = _grad_max -1;
			im1.setPixel(i, line1, _grad[gradPowerIndex]);
			im2.setPixel(i, line2, _grad[gradPowerIndex]);
		}

		//KEW Calculate average
		//_fftinAvg = _fftinSum / 512;
		//System.out.println("KEW fftin Min: " + _fftinMin + ", Avg: " + _fftinAvg + ", Max: " + _fftinMax + ". ");

		Bitmap output;
		
		if (_active_image)
			output = Bitmap.createBitmap(im2,0,line2,512,_imageHeight);		
		else
			output = Bitmap.createBitmap(im1,0,line1,512,_imageHeight);
		
		if (f1 < 0)
			f1 = 0;
		if (f1 >= output.getWidth())
			f1 = output.getWidth()-1;
		if (f2 < 0)
			f2 = 0;
		if (f2 >= output.getWidth())
			f2 = output.getWidth()-1;
		
		int w0,w1,w2,w3;
		w0 = f1-1;
		w1 = f1+1;
		w2 = f2-1;
		w3 = f2+1;
		if (w0 < 0)
			w0 = 0;
		if (w2 < 0)
			w2 = 0;
		if (w1 >= output.getWidth())
			w1 = output.getWidth()-1;
		if (w3 >= output.getWidth())
			w3 = output.getWidth()-1;

		
		for (int i = 0; i < _imageHeight; i++)
		{
			// Draw a black vertical line either side of the green Lines?
			//output.setPixel(w0, i, 0xFF000000);
			//output.setPixel(w1, i, 0xFF000000);
			//output.setPixel(w2, i, 0xFF000000);
			//output.setPixel(w3, i, 0xFF000000);
			// Draw 2 vertical green lines
			output.setPixel(f1, i, 0xFF22FF22);
			output.setPixel(f2, i, 0xFF22FF22);			
		}
		
		line1--;
		line2--;
		
		if (_active_image)
		{
			if (line2 < 0)
			{
				line2 = 399;
				_active_image = false;
			}
		}
		else
		{
			if (line1 < 0)
			{
				line1 = 399;
				_active_image = true;
			}
		}
		
		return output;
	}

	//KEW Allow setting of signalVolume from the AdvancedSettings menu
	public void setSignalVolume(double signalVolume){
    	this._volume = signalVolume;
	}

}
