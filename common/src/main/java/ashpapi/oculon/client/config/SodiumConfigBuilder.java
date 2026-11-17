package ashpapi.oculon.client.config;

import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.StatefulOptionBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class SodiumConfigBuilder {

    public static class Page {
        protected final Component name;
        protected final Group[] groups;

        public Page(Component name, Group... groups) {
            this.name = name;
            this.groups = groups;
        }

        protected OptionPageBuilder create(ConfigBuilder builder, StorageEventHandler saveHandler) {
            OptionPageBuilder page = builder.createOptionPage();
            page.setName(this.name);
            for (Group group : this.groups) {
                page.addOptionGroup(group.create(builder, saveHandler));
            }
            return page;
        }
    }

    public static class Group {
        protected final Option<?, ?, ?>[] options;

        public Group(Option<?, ?, ?>... options) {
            this.options = options;
        }

        protected OptionGroupBuilder create(ConfigBuilder builder, StorageEventHandler saveHandler) {
            OptionGroupBuilder group = builder.createOptionGroup();
            for (Option<?, ?, ?> option : this.options) {
                group.addOption(option.create(builder, saveHandler));
            }
            return group;
        }
    }

    public abstract static class Option<TYPE, OPTION extends Option<TYPE, OPTION, STYPE>, STYPE extends StatefulOptionBuilder<TYPE>> {
        protected final String id;
        protected final Component name;
        protected final Component tooltip;
        protected final Supplier<TYPE> getter;
        protected final Consumer<TYPE> setter;
        protected TYPE defaultValue;

        public Option(String id, Component name, Supplier<TYPE> getter, Consumer<TYPE> setter) {
            this.id = id;
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            if (name.getContents() instanceof TranslatableContents tc) {
                this.tooltip = Component.translatable(tc.getKey() + ".tooltip");
            } else {
                this.tooltip = name;
            }
        }

        @SuppressWarnings("unchecked")
        public OPTION setDefaultValue(TYPE defaultValue) {
            this.defaultValue = defaultValue;
            return (OPTION) this;
        }

        protected abstract STYPE createType(ConfigBuilder builder);

        protected STYPE create(ConfigBuilder builder, StorageEventHandler saveHandler) {
            STYPE option = this.createType(builder);
            option.setName(this.name);
            option.setTooltip(this.tooltip);
            option.setBinding(this.setter, this.getter);
            option.setStorageHandler(saveHandler);
            option.setDefaultValue(this.defaultValue != null ? this.defaultValue : this.getter.get());
            return option;
        }
    }

    public static class IntOption extends Option<Integer, IntOption, IntegerOptionBuilder> {
        protected final Range range;
        protected ControlValueFormatter formatter = v -> Component.literal(Integer.toString(v));

        public IntOption(String id, Component name, Supplier<Integer> getter, Consumer<Integer> setter, Range range) {
            super(id, name, getter, setter);
            this.range = range;
        }

        public IntOption setFormatter(IntFunction<Component> formatter) {
            this.formatter = formatter::apply;
            return this;
        }

        @Override
        protected IntegerOptionBuilder createType(ConfigBuilder builder) {
            return builder.createIntegerOption(Identifier.parse(this.id));
        }

        @Override
        protected IntegerOptionBuilder create(ConfigBuilder builder, StorageEventHandler saveHandler) {
            IntegerOptionBuilder option = super.create(builder, saveHandler);
            option.setRange(this.range);
            option.setValueFormatter(this.formatter);
            return option;
        }
    }

    public static class BoolOption extends Option<Boolean, BoolOption, BooleanOptionBuilder> {
        public BoolOption(String id, Component name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            super(id, name, getter, setter);
        }

        @Override
        protected BooleanOptionBuilder createType(ConfigBuilder builder) {
            return builder.createBooleanOption(Identifier.parse(this.id));
        }
    }

    public static class EnumOption<T extends Enum<T>> extends Option<T, EnumOption<T>, EnumOptionBuilder<T>> {
        private final Class<T> enumClass;
        private Function<T, Component> nameProvider = value -> Component.literal(value == null ? "NULL" : value.toString());

        public EnumOption(String id, Class<T> enumClass, Component name, Supplier<T> getter, Consumer<T> setter) {
            super(id, name, getter, setter);
            this.enumClass = enumClass;
        }

        public EnumOption<T> setNameProvider(Function<T, Component> provider) {
            this.nameProvider = provider;
            return this;
        }

        @Override
        protected EnumOptionBuilder<T> createType(ConfigBuilder builder) {
            return builder.createEnumOption(Identifier.parse(this.id), this.enumClass);
        }

        @Override
        protected EnumOptionBuilder<T> create(ConfigBuilder builder, StorageEventHandler saveHandler) {
            EnumOptionBuilder<T> option = super.create(builder, saveHandler);
            option.setElementNameProvider(this.nameProvider);
            return option;
        }
    }

    public static void buildToSodium(ConfigBuilder builder, ModOptionsBuilder options, StorageEventHandler saveHandler, Page... pages) {
        for (Page page : pages) {
            options.addPage(page.create(builder, saveHandler));
        }
    }
}
