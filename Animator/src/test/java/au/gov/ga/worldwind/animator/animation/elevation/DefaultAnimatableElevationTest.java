package au.gov.ga.worldwind.animator.animation.elevation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import gov.nasa.worldwind.Factory;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.util.WWXML;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import au.gov.ga.worldwind.animator.animation.Animation;
import au.gov.ga.worldwind.animator.animation.KeyFrame;
import au.gov.ga.worldwind.animator.animation.KeyFrameImpl;
import au.gov.ga.worldwind.animator.animation.WorldWindAnimationImpl;
import au.gov.ga.worldwind.animator.animation.io.AnimationFileVersion;
import au.gov.ga.worldwind.animator.animation.parameter.Parameter;
import au.gov.ga.worldwind.animator.animation.parameter.ParameterValue;
import au.gov.ga.worldwind.animator.animation.parameter.ParameterValueFactory;
import au.gov.ga.worldwind.animator.animation.parameter.ParameterValueType;
import au.gov.ga.worldwind.animator.terrain.AnimationElevationLoader;
import au.gov.ga.worldwind.animator.terrain.ElevationModelIdentifierImpl;
import au.gov.ga.worldwind.animator.terrain.exaggeration.ElevationExaggeration;
import au.gov.ga.worldwind.animator.terrain.exaggeration.ElevationExaggerationImpl;
import au.gov.ga.worldwind.animator.util.WorldWindowTestImpl;
import au.gov.ga.worldwind.common.util.XMLUtil;
import au.gov.ga.worldwind.common.util.message.MessageSourceAccessor;
import au.gov.ga.worldwind.common.util.message.StaticMessageSource;
import au.gov.ga.worldwind.test.util.TestUtils;

/**
 * Unit tests for the {@link DefaultAnimatableElevation} class
 * 
 * @author James Navin (james.navin@ga.gov.au)
 *
 */
public class DefaultAnimatableElevationTest
{
	private static final double ALLOWABLE_ERROR = 0.001;
	
	private Mockery mockContext;
	
	private DefaultAnimatableElevation classToBeTested;
	
	private StaticMessageSource messageSource;
	
	private MockElevationFactory elevationFactory;

	private Animation animation;
	
	private ElevationModel elevationModel;
	
	@Before
	public void setup()
	{
		mockContext = new Mockery();
		
		intialiseMessageSource();
		
		animation = new WorldWindAnimationImpl(new WorldWindowTestImpl());
		
		classToBeTested = new DefaultAnimatableElevation(animation);
		
		setupElevationFactory();
	}
	
	@After
	public void teardown()
	{
		AnimationElevationLoader.setElevationFactory(null);
	}

	@Test
	public void testToXml() throws Exception
	{
		addElevationModel("test1", "file://sun-web-common/sandpit/symbolic-links/world-wind/current/dataset/standard/layers/earth_elevation_model.xml");
		
		addExaggerator(1.0, 0.0);
		addExaggerator(2.0, 100.0);
		addExaggerator(3.0, 200.0);
		
		addKeyFrame(0, 1.2, getParameter(0));
		addKeyFrame(10, 2.5, getParameter(0));
		
		addKeyFrame(20, 3.5, getParameter(2));
		
		Document xmlDocument = writeClassToBeTestedToXml();
		
		ByteArrayOutputStream resultStream = writeDocumentToStream(xmlDocument);
		
		String result = normalise(new String(resultStream.toByteArray()));
		String expected = normalise(TestUtils.readStreamToString(getClass().getResourceAsStream("animatableElevationXmlSnippet.xml")));
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testFromXml() throws Exception
	{
		AVList context = new AVListImpl();
		context.setValue(AnimationFileVersion.VERSION020.getConstants().getAnimationKey(), animation);
		AnimationFileVersion versionId = AnimationFileVersion.VERSION020;
		
		// Setup the elements correctly (add an 'AnimatableObjects' element
		Document document = WWXML.openDocument(getClass().getResourceAsStream("animatableElevationXmlSnippet.xml"));
		Element elevationElement = WWXML.getElement(document.getDocumentElement(), "//" + versionId.getConstants().getAnimatableElevationElementName(), null);
		
		DefaultAnimatableElevation result = (DefaultAnimatableElevation)classToBeTested.fromXml(elevationElement, versionId, context);
		
		assertNotNull(result);
		assertNotNull(result.getRootElevationModel());
		assertElevationModelCorrect(result);
		assertElevationExaggeratorsCorrect(result);
		assertElevationParametersCorrect(result);
		assertKeyFramesCorrect(result);
	}

	private void assertKeyFramesCorrect(DefaultAnimatableElevation result)
	{
		assertEquals(3, animation.getKeyFrameCount());

		ArrayList<Parameter> parameterList = new ArrayList<Parameter>(result.getParameters());
		
		List<KeyFrame> keyFrames = animation.getKeyFrames(parameterList.get(0));
		assertEquals(2, keyFrames.size());
		assertEquals(0, keyFrames.get(0).getFrame());
		assertEquals(10, keyFrames.get(1).getFrame());
		
		keyFrames = animation.getKeyFrames(parameterList.get(1));
		assertEquals(0, keyFrames.size());
		
		keyFrames = animation.getKeyFrames(parameterList.get(2));
		assertEquals(1, keyFrames.size());
		assertEquals(20, keyFrames.get(0).getFrame());
	}

	private void assertElevationParametersCorrect(DefaultAnimatableElevation result)
	{
		assertEquals(3, result.getEnabledParameters().size());
		ArrayList<Parameter> parameterList = new ArrayList<Parameter>(result.getParameters());
		assertElevationParameterCorrect(parameterList.get(0), "Exaggeration", true, 1.0);
		assertElevationParameterCorrect(parameterList.get(1), "Exaggeration", true, 2.0);
		assertElevationParameterCorrect(parameterList.get(2), "Exaggeration", true, 3.0);
	}

	private void assertElevationParameterCorrect(Parameter parameter, String name, boolean enabled, double defaultValue)
	{
		assertEquals(name, parameter.getName());
		assertEquals(enabled, parameter.isEnabled());
		assertEquals(defaultValue, parameter.getDefaultValue(0), ALLOWABLE_ERROR);
	}

	private void assertElevationExaggeratorsCorrect(DefaultAnimatableElevation result)
	{
		assertEquals(3, result.getRootElevationModel().getExaggerators().size());
		assertExaggeratorCorrect(result.getRootElevationModel().getExaggerators().get(0), 0.0, 1.0);
		assertExaggeratorCorrect(result.getRootElevationModel().getExaggerators().get(1), 100.0, 2.0);
		assertExaggeratorCorrect(result.getRootElevationModel().getExaggerators().get(2), 200.0, 3.0);
	}

	private void assertExaggeratorCorrect(ElevationExaggeration elevationExaggeration, double boundary, double exaggeration)
	{
		assertEquals(boundary, elevationExaggeration.getElevationBoundary(), ALLOWABLE_ERROR);
		assertEquals(exaggeration, elevationExaggeration.getExaggeration(), ALLOWABLE_ERROR);
	}

	private void assertElevationModelCorrect(DefaultAnimatableElevation result)
	{
		assertEquals(1, result.getElevationModelIdentifiers().size());
		assertEquals("test1", result.getElevationModelIdentifiers().get(0).getName());
		assertEquals("file://sun-web-common/sandpit/symbolic-links/world-wind/current/dataset/standard/layers/earth_elevation_model.xml", result.getElevationModelIdentifiers().get(0).getLocation());
	}

	private void addElevationModel(String name, String location)
	{
		classToBeTested.addElevationModel(new ElevationModelIdentifierImpl(name, location));
	}

	private void addExaggerator(double exaggeration, double boundary)
	{
		classToBeTested.addElevationExaggerator(new ElevationExaggerationImpl(exaggeration, boundary));
	}

	private ByteArrayOutputStream writeDocumentToStream(Document xmlDocument)
	{
		ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
		XMLUtil.saveDocumentToFormattedStream(xmlDocument, resultStream );
		return resultStream;
	}

	private Document writeClassToBeTestedToXml()
	{
		Document xmlDocument = WWXML.createDocumentBuilder(false).newDocument();
		Element animatableObjectsElement = xmlDocument.createElement("animatableObjects");
		Element xmlElement = classToBeTested.toXml(animatableObjectsElement, AnimationFileVersion.VERSION020);
		assertNotNull(xmlElement);
		animatableObjectsElement.appendChild(xmlElement);
		xmlDocument.appendChild(animatableObjectsElement);
		return xmlDocument;
	}
	
	private void addKeyFrame(int frame, double value, Parameter layerParameter)
	{
		ParameterValue paramValue = ParameterValueFactory.createParameterValue(ParameterValueType.LINEAR, layerParameter, value, frame);
		KeyFrame keyFrame = new KeyFrameImpl(frame, Arrays.asList(new ParameterValue[]{paramValue}));
		animation.insertKeyFrame(keyFrame);
	}
	
	private Parameter getParameter(int i)
	{
		return new ArrayList<Parameter>(classToBeTested.getParameters()).get(i);
	}
	
	private void intialiseMessageSource()
	{
		messageSource = new StaticMessageSource();
		MessageSourceAccessor.set(messageSource);
	}
	
	private void setupElevationFactory()
	{
		elevationModel = mockContext.mock(ElevationModel.class);
		mockContext.checking(new Expectations()
		{{
			allowing(elevationModel).setValue(with(any(String.class)), with(anything()));
			allowing(elevationModel).setName(with(any(String.class)));
		}});
		
		elevationFactory = new MockElevationFactory();
		elevationFactory.setResult(elevationModel);
		AnimationElevationLoader.setElevationFactory(elevationFactory);
	}
	
	private String normalise(String target)
	{
		return target.trim().replace("\r\n", "\n");
	}
	
	/**
	 * A mock layer factory that can have results set on it for testing purposes.
	 */
	private static class MockElevationFactory implements Factory
	{
		private Object result = null;
		
		@Override
		public Object createFromConfigSource(Object configSource, AVList params)
		{
			return result;
		}
	
		public void setResult(Object result)
		{
			this.result = result;
		}
		
	}
}
