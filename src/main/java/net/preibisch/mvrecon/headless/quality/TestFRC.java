/*-
 * #%L
 * Software for the reconstruction of multi-view microscopic acquisitions
 * like Selective Plane Illumination Microscopy (SPIM) Data.
 * %%
 * Copyright (C) 2012 - 2017 Multiview Reconstruction developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package net.preibisch.mvrecon.headless.quality;

import java.io.File;
import java.util.Random;

import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.io.IOFunctions;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;
import net.preibisch.mvrecon.process.export.DisplayImage;
import net.preibisch.mvrecon.process.quality.FRC;
import net.preibisch.mvrecon.process.quality.FRC.ThresholdMethod;
import net.preibisch.mvrecon.process.quality.FRCRealRandomAccessible;

public class TestFRC
{
	public static void main( String[] args ) throws SpimDataException
	{
		new ImageJ();

		//final Img< FloatType > img = IOFunctions.openAs32BitArrayImg( new File( "/Users/spreibi/Documents/Microscopy/SPIM/HisYFP-SPIM/spim_TL18_Angle0.tif" ) );
		final Img< FloatType > img = IOFunctions.openAs32BitArrayImg( new File(
				"/Users/spreibi/Documents/BIMSB/Projects/CLARITY/Quality assessment/clarity-2.tif" ) );
		final ImagePlus imp = DisplayImage.getImagePlusInstance( img, true, "brain", Double.NaN, Double.NaN );
		imp.show();

		testFRCOld( img );
	}

	public static void testFRC( final Img< FloatType > input )
	{
		final FRCRealRandomAccessible< FloatType > frc = FRCRealRandomAccessible.distributeGridFRC( input, 0.1, 10, 256, true, null );
		//final FRCRealRandomAccessible< FloatType > frc = FRCRealRandomAccessible.fixedGridFRC( input, 50, 5, 256, false, null );

		DisplayImage.getImagePlusInstance( frc.getRandomAccessibleInterval(), false, "frc", Double.NaN, Double.NaN ).show();
	}

	public static void testFRCOld( final Img< FloatType > img )
	{
		ThresholdMethod tm = ThresholdMethod.FIXED_1_OVER_7;
		ImageStack stack = null;

		for ( int z = 0; z < img.dimension( 2 ) - 1; z += 10 )
		{
			final Pair< FloatProcessor, FloatProcessor > fps1 = getTwoImagesA( img, z, 1 );
			final Pair< FloatProcessor, FloatProcessor > fps10 = getTwoImagesA( img, z - 5, 10 );

			//new ImagePlus( "a", fps.getA() ).show();
			//new ImagePlus( "b", fps.getB() ).show();
			//SimpleMultiThreading.threadHaltUnClean();
	
			final FRC frc = new FRC();

			// Get FIRE Number, assumes you have access to the two image processors.
			double[][] frcCurve1 = frc.calculateFrcCurve( fps1.getA(), fps1.getB() );
			double[][] frcCurve10 =  frc.getSmoothedCurve( frc.calculateFrcCurve( fps10.getA(), fps10.getB() ) );

			final double[][] frcCurve = frcCurve1.clone();
			for ( int i = 0; i < frcCurve.length; ++i )
				frcCurve[ i ][ 1 ] = Math.max( 0, frcCurve1[ i ][ 1 ] - frcCurve10[ i ][ 1 ] );

			double integral = FRCRealRandomAccessible.integral( frcCurve );

			double fire = frc.calculateFireNumber( frcCurve, tm );
			System.out.println( z + ": " + fire + " " + integral);

			//if ( z== 41 || z== 42 )
			{
			Plot p = frc.doPlot( frcCurve, frc.getSmoothedCurve( frcCurve ), tm, fire, "" + z );
			ImageProcessor ip = p.getImagePlus().getProcessor();
			if ( stack == null )
				stack = new ImageStack( ip.getWidth(), ip.getHeight() );
			stack.addSlice( ip );
			//p.show();
			}

			
			
			//break;
		}
		
		new ImagePlus( "fd", stack ).show();
	}

	public static Pair< FloatProcessor, FloatProcessor > getTwoImagesA( final Img< FloatType > imgIn, final int z, final int dist )
	{
		final RandomAccessible< FloatType > r = Views.extendMirrorSingle( imgIn );
		final RandomAccessibleInterval< FloatType > img = Views.interval( r, imgIn );

		final RandomAccessibleInterval< FloatType > s0 = Views.hyperSlice( img, 2, z );
		final RandomAccessibleInterval< FloatType > s1 = Views.hyperSlice( img, 2, z + dist );

		final FloatProcessor fp0 = new FloatProcessor( (int)s0.dimension( 0 ), (int)s0.dimension( 1 ) );
		final FloatProcessor fp1 = new FloatProcessor( (int)s0.dimension( 0 ), (int)s0.dimension( 1 ) );

		final Cursor< FloatType > c0 = Views.iterable( s0 ).localizingCursor();
		final Cursor< FloatType > c1 = Views.iterable( s1 ).localizingCursor();

		while ( c0.hasNext() )
		{
			c0.fwd();
			c1.fwd();

			fp0.setf( c0.getIntPosition( 0 ), c0.getIntPosition( 1 ), c0.get().get() );
			fp1.setf( c1.getIntPosition( 0 ), c1.getIntPosition( 1 ), c1.get().get() );
		}

		return new ValuePair< FloatProcessor, FloatProcessor >( fp0, fp1 );
	}

	public static Pair< FloatProcessor, FloatProcessor > getTwoImagesB( final Img< FloatType > img, final int z )
	{
		Random rnd = new Random( System.currentTimeMillis() );
		final RandomAccessibleInterval< FloatType > s0 = Views.hyperSlice( img, 2, z );

		final FloatProcessor fp0 = new FloatProcessor( (int)s0.dimension( 0 ), (int)s0.dimension( 1 ) );
		final FloatProcessor fp1 = new FloatProcessor( (int)s0.dimension( 0 ), (int)s0.dimension( 1 ) );

		final Cursor< FloatType > c0 = Views.iterable( s0 ).localizingCursor();

		while ( c0.hasNext() )
		{
			c0.fwd();

			final int x = c0.getIntPosition( 0 );
			final int y = c0.getIntPosition( 1 );

			if ( (x+y) % 2 == 0 )
			//if ( rnd.nextInt() % 2 == 0 )
				fp0.setf( x, y, c0.get().get() );
			else
				fp1.setf( x, y, c0.get().get() );
		}

		return new ValuePair< FloatProcessor, FloatProcessor >( fp0, fp1 );
	}
}