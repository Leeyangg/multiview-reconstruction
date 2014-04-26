package spim.process.fusion.deconvolution;

import net.imglib2.type.numeric.real.FloatType;
import mpicbg.spim.data.sequence.Channel;

public class ChannelPSF
{
	final Channel channel;
	final String label;
	final Channel otherChannel;
	ExtractPSF< FloatType > extractPSF;
	
	public ChannelPSF( final Channel channel, final String label )
	{
		this.channel = channel;
		this.label = label;
		this.otherChannel = null;
	}
	
	public ChannelPSF( final Channel channel, final Channel otherPSF )
	{
		this.channel = channel;
		this.label = null;
		this.otherChannel = otherPSF;
	}
	
	public boolean isExtractedPSF() { return label != null;	}
	public Channel getChannel() { return channel; }
	public String getLabel() { return label; }
	public Channel getOtherChannel() { return otherChannel; }
	
	public void setExtractPSFInstance( final ExtractPSF< FloatType > e ) { this.extractPSF = e; }
	public ExtractPSF< FloatType > getExtractPSFInstance(){ return extractPSF; }
}
