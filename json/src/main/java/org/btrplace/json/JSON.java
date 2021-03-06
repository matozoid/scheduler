/*
 * Copyright (c) 2016 University Nice Sophia Antipolis
 *
 * This file is part of btrplace.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.btrplace.json;

import org.btrplace.json.model.InstanceConverter;
import org.btrplace.json.plan.ReconfigurationPlanConverter;
import org.btrplace.model.Instance;
import org.btrplace.plan.ReconfigurationPlan;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class to ease the serialisation and the deserialisation of the main btrplace entities.
 *
 * @author Fabien Hermenier
 */
public class JSON {

    private JSON() {
    }

    private static InputStreamReader makeIn(File f) throws IOException {
        if (f.getName().endsWith(".gz")) {
            return new InputStreamReader(new GZIPInputStream(new FileInputStream(f)), UTF_8);
        }
        return new InputStreamReader(new FileInputStream(f), UTF_8);
    }

    private static OutputStreamWriter makeOut(File f) throws IOException {
        if (f.getName().endsWith(".gz")) {
            return new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)), UTF_8);
        }
        return new OutputStreamWriter(new FileOutputStream(f), UTF_8);
    }

    /**
     * Read an instance from a file.
     * A file ending with '.gz' is uncompressed first
     *
     * @param f the file to parse
     * @return the resulting instance
     * @throws IllegalArgumentException if an error occurred while reading the file
     */
    public static Instance readInstance(File f) {
        try (Reader in = makeIn(f)) {
            return readInstance(in);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Read an instance.
     *
     * @param r the stream to read
     * @return the resulting instance
     * @throws IllegalArgumentException if an error occurred while reading the json
     */
    public static Instance readInstance(Reader r) {
        try {
            InstanceConverter c = new InstanceConverter();
            return c.fromJSON(r);
        } catch (JSONConverterException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Write an instance.
     *
     * @param instance the instance to write
     * @param f        the output file. If it ends with '.gz' it will be gzipped
     * @throws IllegalArgumentException if an error occurred while writing the json
     */
    public static void write(Instance instance, File f) {
        try (OutputStreamWriter out = makeOut(f)) {
            write(instance, out);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Write an instance
     *
     * @param instance the instance to write
     * @param a        the stream to write on.
     * @throws IllegalArgumentException if an error occurred while writing the json
     */
    public static void write(Instance instance, Appendable a) {
        try {
            InstanceConverter c = new InstanceConverter();
            c.toJSON(instance).writeJSONString(a);
        } catch (IOException | JSONConverterException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Serialise a instance.
     *
     * @param instance the instance to write
     * @throws IllegalArgumentException if an error occurred while writing the json
     */
    public static String toString(Instance instance) {
        try {
            InstanceConverter c = new InstanceConverter();
            return c.toJSON(instance).toJSONString();
        } catch (JSONConverterException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /**
     * Read a reconfiguration plan from a file.
     * A file ending with '.gz' is uncompressed first
     *
     * @param f the file to parse
     * @return the resulting plan
     * @throws IllegalArgumentException if an error occurred while reading the file
     */
    public static ReconfigurationPlan readReconfigurationPlan(File f) {
        try (Reader in = makeIn(f)) {
            return readReconfigurationPlan(in);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Read a plan.
     *
     * @param r the stream to read
     * @return the resulting reconfiguration plan
     * @throws IllegalArgumentException if an error occurred while reading the json
     */
    public static ReconfigurationPlan readReconfigurationPlan(Reader r) {
        try {
            ReconfigurationPlanConverter c = new ReconfigurationPlanConverter();
            return c.fromJSON(r);
        } catch (JSONConverterException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Write a reconfiguration plan.
     *
     * @param plan the reconfiguration plan to write
     * @param f    the output file. If it ends with '.gz' it will be gzipped
     * @throws IllegalArgumentException if an error occurred while writing the json
     */
    public static void write(ReconfigurationPlan plan, File f) {
        try (OutputStreamWriter out = makeOut(f)) {
            write(plan, out);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Write a reconfiguration plan.
     *
     * @param plan the plan to write
     * @param a    the stream to write on.
     * @throws IllegalArgumentException if an error occurred while writing the json
     */
    public static void write(ReconfigurationPlan plan, Appendable a) {
        try {
            ReconfigurationPlanConverter c = new ReconfigurationPlanConverter();
            c.toJSON(plan).writeJSONString(a);
        } catch (IOException | JSONConverterException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Serialise a reconfiguration plan.
     *
     * @param plan the plan to write
     * @throws IllegalArgumentException if an error occurred while writing the json
     */
    public static String toString(ReconfigurationPlan plan) {
        try {
            ReconfigurationPlanConverter c = new ReconfigurationPlanConverter();
            return c.toJSON(plan).toJSONString();
        } catch (JSONConverterException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
