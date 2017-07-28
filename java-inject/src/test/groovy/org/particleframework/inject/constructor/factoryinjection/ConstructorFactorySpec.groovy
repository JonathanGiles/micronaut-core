package org.particleframework.inject.constructor.factoryinjection

import org.particleframework.context.BeanContext
import org.particleframework.context.DefaultBeanContext
import spock.lang.Specification
/**
 * Created by graemerocher on 12/05/2017.
 */
class ConstructorFactorySpec extends Specification {

    void "test injection with constructor supplied by a provider"() {
        given:
        BeanContext context = new DefaultBeanContext()
        context.start()

        when:"A bean is obtained which has a constructor that depends on a bean provided by a provider"
        B b =  context.getBean(B)

        then:"The implementation is injected"
        b.a != null
        b.a instanceof AImpl
        b.a.c != null
        b.a.c2 != null
        b.a.d != null
        b.a.is(context.getBean(AImpl))
    }
}