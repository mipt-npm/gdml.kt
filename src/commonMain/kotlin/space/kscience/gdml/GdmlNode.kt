@file:UseSerializers(NumberSerializer::class)

package space.kscience.gdml

import kotlinx.serialization.*
import nl.adaptivity.xmlutil.serialization.XmlPolyChildren
import nl.adaptivity.xmlutil.serialization.XmlSerialName

public interface GdmlNode {
    public var name: String
}

@Serializable
public data class GdmlRef<out T : GdmlNode>(var ref: String)

public fun <T : GdmlNode> ref(ref: String): GdmlRef<T> {
    return GdmlRef<T>(ref)
}

/**
 * Get a ref to this node
 */
public fun <T : GdmlNode> T.ref(): GdmlRef<T> =
    if (name.isBlank()) error("Can't produce a ref for anonymous node") else GdmlRef(name)

// define block members


//Structure elements
@Serializable
public sealed class GdmlPlacement

/**
 * Does not iherit [GdmlNode] since it does not have a name and could not be referenced
 */
@Serializable
@SerialName("physvol")
@GdmlApi
public class GdmlPhysVolume(
    public var name: String = "@undefined",
    @XmlSerialName("volumeref", "", "")
    public var volumeref: GdmlRef<GdmlGroup>,
) : GdmlPlacement() {
    public var copynumber: Int? = null

    @XmlSerialName("position", "", "")
    public var position: GdmlPosition? = null

    @XmlSerialName("positionref", "", "")
    public var positionref: GdmlRef<GdmlPosition>? = null

    @XmlSerialName("rotation", "", "")
    public var rotation: GdmlRotation? = null

    @XmlSerialName("rotationref", "", "")
    public var rotationref: GdmlRef<GdmlRotation>? = null

    @XmlSerialName("scale", "", "")
    public var scale: GdmlScale? = null

    @XmlSerialName("scaleref", "", "")
    public var scaleref: GdmlRef<GdmlScale>? = null
}

/**
 * Get the position from either position block or reference (if root is provided)
 */
public fun GdmlPhysVolume.resolvePosition(root: Gdml): GdmlPosition? = position ?: positionref?.resolve(root)

/**
 * Get the rotation from either position block or reference (if root is provided)
 */
public fun GdmlPhysVolume.resolveRotation(root: Gdml): GdmlRotation? = rotation ?: rotationref?.resolve(root)

/**
 * Get the scale from either position block or reference (if root is provided)
 */
public fun GdmlPhysVolume.resolveScale(root: Gdml): GdmlScale? = scale ?: scaleref?.resolve(root)

public fun GdmlPhysVolume.position(
    x: Number = 0f,
    y: Number = 0f,
    z: Number = 0f,
    block: GdmlPosition.() -> Unit = {},
) {
    position = GdmlPosition("$name.position", x, y, z).apply(block)
}

public fun GdmlPhysVolume.rotation(
    x: Number = 0f,
    y: Number = 0f,
    z: Number = 0f,
    block: GdmlRotation.() -> Unit = {},
) {
    rotation = GdmlRotation("$name.rotation", x, y, z).apply(block)
}

public fun GdmlPhysVolume.scale(x: Number = 0f, y: Number = 0f, z: Number = 0f, block: GdmlScale.() -> Unit = {}) {
    scale = GdmlScale("$name.scale", x, y, z).apply(block)
}


/**
<...
axis=" xs:string [1]"
number=" ExpressionOrIDREFType [1]"
width=" ExpressionOrIDREFType [1]"
offset=" ExpressionOrIDREFType [1]"
unit=" xs:string [0..1]">
<volumeref> ReferenceType </volumeref> [1]
</...>
 */
@Serializable
@SerialName("divisionvol")
@GdmlApi
public class GdmlDivisionVolume(
    public var axis: String,
    public var number: Number,
    public var width: Number,
    public var offset: Number,
    @XmlSerialName("volumeref", "", "")
    public var volumeref: GdmlRef<GdmlVolume>,
    public var unit: String = "mm",
) : GdmlPlacement()

@Serializable
@GdmlApi
public sealed class GdmlGroup : GdmlNode {
    @XmlSerialName("physvol", "", "")
    public val physVolumes: ArrayList<GdmlPhysVolume> = ArrayList()

    @Transient
    private var autoNameCounter = 0

    public fun physVolume(
        volumeref: GdmlRef<GdmlGroup>,
        name: String = "${volumeref.ref}-${(this.name.hashCode() + autoNameCounter++).toUInt().toString(16)}",
        block: GdmlPhysVolume.() -> Unit = {},
    ): GdmlPhysVolume {
        name.validateNCName()
        if (physVolumes.find { it.name == name } != null) error("PhysVolume with name $name redeclaration at volume ${this.name}")
        val res = GdmlPhysVolume(name, volumeref).apply(block)
        physVolumes.add(res)
        return res
    }
}

//public fun GdmlGroup.physVolume(volume: GdmlGroup, block: GdmlPhysVolume.() -> Unit): GdmlPhysVolume =
//    physVolume(volume.ref(), block)

@Serializable
@SerialName("assembly")
@GdmlApi
public class GdmlAssembly(override var name: String) : GdmlGroup()

@Serializable
@SerialName("volume")
@GdmlApi
public class GdmlVolume(
    override var name: String,
    @XmlSerialName("materialref", "", "")
    public var materialref: GdmlRef<GdmlMaterial>,
    @XmlSerialName("solidref", "", "")
    public var solidref: GdmlRef<GdmlSolid>,
) : GdmlGroup() {

    @XmlPolyChildren(arrayOf("physvol", "divisionvol"))
    @Polymorphic
    public var placement: GdmlPlacement? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GdmlVolume

        if (name != other.name) return false
        if (materialref != other.materialref) return false
        if (solidref != other.solidref) return false
        if (placement != other.placement) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + materialref.hashCode()
        result = 31 * result + solidref.hashCode()
        result = 31 * result + (placement?.hashCode() ?: 0)
        return result
    }

}